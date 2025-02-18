package io.snabble.sdk.payment;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.RestrictTo;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import io.snabble.sdk.Environment;
import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.Utils;
import io.snabble.sdk.utils.security.KeyStoreCipher;

/**
 * Class for managing and storing payment credentials.
 */
public class PaymentCredentialsStore {
    private class Data {
        private List<PaymentCredentials> credentialsList;
        private String id;
        private boolean removedOldCreditCards;
        private boolean isMigratedFromKeyStore;
        private boolean isKeyguarded;
    }

    private SharedPreferences sharedPreferences;
    private Data data;
    private final List<Callback> callbacks = new CopyOnWriteArrayList<>();
    private final List<OnPaymentCredentialsAddedListener> onPaymentCredentialsAddedListeners = new CopyOnWriteArrayList<>();
    private String credentialsKey;

    // this still is needed for migration
    KeyStoreCipher keyStoreCipher;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public PaymentCredentialsStore() {

    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void init(Context context, Environment environment) {
        sharedPreferences = context.getSharedPreferences("snabble_payment", Context.MODE_PRIVATE);
        credentialsKey = "credentials_" + (environment != null ? environment.name() : "_UNKNOWN");

        load();
        initializeKeyStore();

        if (data.id == null) {
            generateRandomId();
        }
    }

    @SuppressLint("NewApi")
    private void initializeKeyStore() {
        if (keyStoreCipher == null) {
            keyStoreCipher = KeyStoreCipher.create(Snabble.getInstance().getApplication(), "SnabblePaymentCredentialsStore", true);
        }
    }

    private void ensureKeyStoreIsAccessible() {
        initializeKeyStore();

        if (keyStoreCipher != null) {
            keyStoreCipher.validate();

            String id = keyStoreCipher.id();
            if (id == null) {
                Logger.errorEvent("Keystore has no id!");
                keyStoreCipher = null;
                return;
            }

            if (!id.equals(data.id)) {
                data.id = keyStoreCipher.id();
                data.isKeyguarded = true;
                removeInvalidCredentials();
            }
        }

        Context context = Snabble.getInstance().getApplication();
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        boolean secure = false;

        if (keyguardManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                secure = keyguardManager.isDeviceSecure(); // ignores SIM lock, API 23+
            } else {
                secure = keyguardManager.isKeyguardSecure();
            }
        }

        if (!secure) {
            Logger.errorEvent("Removing payment credentials, because device is not secure. KeyguardManager is " + (keyguardManager != null ? "not null" : "null"));
            removeInvalidCredentials();
        }
    }

    private void generateRandomId() {
        data.id = Utils.sha1Hex(Long.toString(System.currentTimeMillis()));
        data.isKeyguarded = false;
    }

    /**
     * Validate and get list of all payment credentials stored by the user. Also validates
     * certificate chains.
     *
     * For displaying a list of credentials use {@link #getAllWithoutKeyStoreValidation()}
     */
    public List<PaymentCredentials> getAll() {
        validate();
        ensureKeyStoreIsAccessible();
        return Collections.unmodifiableList(data.credentialsList);
    }

    /**
     * @return Returns a list of PaymentCredentials that may not be accessible.
     *
     * You can use this method for listing payment methods, because it does not do validation
     * which can be slow
     */
    public List<PaymentCredentials> getAllWithoutKeyStoreValidation() {
        return Collections.unmodifiableList(data.credentialsList);
    }

    /**
     * The unique identifier of this storage
     */
    public String id() {
        ensureKeyStoreIsAccessible();
        return data.id;
    }

    /**
     * @return true if old credit cards before v0.33.0 were added and therefore rendered unusable and deleted
     */
    public boolean hasRemovedOldCreditCards() {
        return data.removedOldCreditCards;
    }

    /**
     * Add and persist payment credentials
     */
    public synchronized void add(PaymentCredentials credentials) {
        if (credentials == null) {
            return;
        }

        ensureKeyStoreIsAccessible();

        data.credentialsList.add(credentials);
        save();
        notifyPaymentCredentialsAdded(credentials);
        notifyChanged();
    }

    public synchronized void justEmitCredentials(final PaymentCredentials credentials) {
        ((MutableCredentialsFlow) PaymentCredentialsFlow.INSTANCE).emitCredentials(credentials);
    }

    /**
     * Remove and persist payment credentials
     */
    public synchronized void remove(PaymentCredentials credentials) {
        if (data.credentialsList.remove(credentials)) {
            save();
            notifyChanged();
        }
    }

    /**
     * Remove all credentials that are not valid anymore (e.g. expired credit cards)
     */
    public synchronized void removeInvalidCredentials() {
        // we do not lose payment information if we are not using key store
        if (data.isMigratedFromKeyStore) {
            return;
        }

        Logger.errorEvent("Removing payment credentials, because key store id differs");
        
        List<PaymentCredentials> removals = new ArrayList<>();
        for (PaymentCredentials credentials : data.credentialsList) {
            if (!credentials.canBypassKeyStore()) {
                removals.add(credentials);
            }
        }

        for (PaymentCredentials credentials : removals) {
            data.credentialsList.remove(credentials);
        }

        if (removals.size() > 0) {
            generateRandomId();
            save();
            notifyChanged();

            Snabble snabble = Snabble.getInstance();
            if (snabble.getProjects().size() > 0) {
                if (removals.size() > 0) {
                    Logger.errorEvent("Deleted payment credentials because device is not secure anymore. Lost access to %d payment credentials", removals.size());
                }
            }
        }
    }

    /**
     * Removes all payment credentials
     */
    public synchronized void clear() {
        if (data.credentialsList.size() > 0) {
            data.credentialsList.clear();
            save();
            notifyChanged();
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public synchronized void maybeMigrateKeyStoreCredentials() {
        if (!data.isMigratedFromKeyStore) {
            List<PaymentCredentials> removals = new ArrayList<>();

            for (PaymentCredentials paymentCredentials : data.credentialsList) {
                if (!paymentCredentials.migrateFromKeyStore()) {
                    removals.add(paymentCredentials);
                }
            }

            for (PaymentCredentials credentials : removals) {
                data.credentialsList.remove(credentials);
            }

            data.isMigratedFromKeyStore = true;
            save();
        }
    }

    private synchronized void save() {
        Gson gson = new Gson();
        String json = gson.toJson(data);
        sharedPreferences.edit().putString(credentialsKey, json).apply();
    }

    private synchronized void load() {
        Gson gson = new Gson();
        data = new Data();
        data.credentialsList = new ArrayList<>();

        String json = sharedPreferences.getString(credentialsKey, null);
        if (json != null) {
            try {
                data = gson.fromJson(json, Data.class);
                // Filter only known types
                data.credentialsList = data.credentialsList.stream()
                        .filter(paymentCredentials -> paymentCredentials.getType() != null)
                        .collect(Collectors.toList());
                save();
            } catch (Exception e) {
                Logger.errorEvent("Could not read payment credentials: %s", e.getMessage());
            }

            if (data.credentialsList == null) {
                data.credentialsList = new ArrayList<>();
            }

            validate();
        }
    }

    private synchronized void validate() {
        final List<PaymentCredentials> credentialsList = data.credentialsList;
        if (credentialsList == null || credentialsList.isEmpty()) return;

        boolean changed = false;

        List<PaymentCredentials> removals = new ArrayList<>();

        for (PaymentCredentials credentials : credentialsList) {
            if (credentials != null) {
                if (!credentials.validate()) {
                    removals.add(credentials);

                    if (credentials.getType() == PaymentCredentials.Type.CREDIT_CARD) {
                        data.removedOldCreditCards = true;
                    }

                    changed = true;
                } else {
                    // app id's were not stored in old versions, if its not there assume the
                    // current app id was the app id in which the payment method was created
                    if (credentials.checkAppId()) {
                        changed = true;
                    }
                }
            }
        }

        for (PaymentCredentials credentials : removals) {
            credentialsList.remove(credentials);
        }

        // old apps don't have ids set
        for (PaymentCredentials pc : data.credentialsList) {
            if (pc != null && pc.getId() == null) {
                pc.generateId();
                changed = true;
            }
        }

        if (changed) {
            save();
        }
    }

    /**
     * Get the number of payment credentials of a given project
     */
    public int getCountForProject(Project project) {
        int count = 0;

        HashSet<PaymentMethod> onlineMethods = new HashSet<>();
        for (PaymentMethod paymentMethod : project.getAvailablePaymentMethods()) {
            if (paymentMethod.isRequiringCredentials()) {
                onlineMethods.add(paymentMethod);
            }
        }

        for (PaymentCredentials pc : data.credentialsList) {
            if (pc.getAppId().equals(Snabble.getInstance().getConfig().appId)) {
                String projectId = pc.getProjectId();
                if (projectId != null) {
                    if (pc.getProjectId().equals(project.getId()) && onlineMethods.contains(pc.getPaymentMethod())) {
                        count++;
                    }
                } else {
                    if (pc.getType() == PaymentCredentials.Type.SEPA && onlineMethods.contains(pc.getPaymentMethod())) {
                        count++;
                    }
                }
            }
        }

        if (project.getGooglePayHelper() != null
                && project.getGooglePayHelper().isGooglePayAvailable()) {
            count++;
        }

        return count;
    }

    /**
     * Get the number of payment credentials that are usable
     */
    public int getUsablePaymentCredentialsCount() {
        int i = 0;
        for (PaymentCredentials pc : data.credentialsList) {
            if (pc.getAppId().equals(Snabble.getInstance().getConfig().appId)) {
                i++;
            }
        }
        return i;
    }

    /**
     * Interface for getting notified when payment credentials are added
     */
    public interface OnPaymentCredentialsAddedListener {
        void onAdded(PaymentCredentials paymentCredentials);
    }

    private void notifyPaymentCredentialsAdded(PaymentCredentials pc) {
        Dispatch.mainThread(() -> {
            for (OnPaymentCredentialsAddedListener l : onPaymentCredentialsAddedListeners) {
                l.onAdded(pc);
            }
        });
    }

    public void addOnPaymentCredentialsAddedListener(OnPaymentCredentialsAddedListener onPaymentCredentialsAddedListener) {
        if (!onPaymentCredentialsAddedListeners.contains(onPaymentCredentialsAddedListener)) {
            onPaymentCredentialsAddedListeners.add(onPaymentCredentialsAddedListener);
        }
    }

    public void removeOnPaymentCredentialsAddedListener(OnPaymentCredentialsAddedListener onPaymentCredentialsAddedListener) {
        onPaymentCredentialsAddedListeners.remove(onPaymentCredentialsAddedListener);
    }

    /**
     * Interface for getting notified when payment credentials are changed
     */
    public interface Callback {
        void onChanged();
    }

    private void notifyChanged() {
        Dispatch.mainThread(() -> {
            for (Callback cb : callbacks) {
                cb.onChanged();
            }
        });
    }

    /**
     * Adds a callback to the payment credentials store
     */
    public void addCallback(Callback cb) {
        if (!callbacks.contains(cb)) {
            callbacks.add(cb);
        }
    }

    /**
     * Removes a callback to the payment credentials store
     */
    public void removeCallback(Callback cb) {
        callbacks.remove(cb);
    }
}
