package io.snabble.sdk.payment;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.snabble.sdk.Environment;
import io.snabble.sdk.Events;
import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.UserPreferences;
import io.snabble.sdk.utils.Dispatch;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.Utils;
import io.snabble.sdk.utils.security.KeyStoreCipher;

public class PaymentCredentialsStore {
    private class Data {
        private List<PaymentCredentials> credentialsList;
        private String id;
        private boolean removedOldCreditCards;
        private boolean isKeyguarded;
    }

    private SharedPreferences sharedPreferences;
    private Data data;
    private List<Callback> callbacks = new CopyOnWriteArrayList<>();
    private List<OnPaymentCredentialsAddedListener> onPaymentCredentialsAddedListeners = new CopyOnWriteArrayList<>();
    private String credentialsKey;
    private UserPreferences userPreferences;

    KeyStoreCipher keyStoreCipher;

    public PaymentCredentialsStore() {

    }

    public void init(Context context, Environment environment) {
        sharedPreferences = context.getSharedPreferences("snabble_payment", Context.MODE_PRIVATE);
        credentialsKey = "credentials_" + (environment != null ? environment.name() : "_UNKNOWN");
        userPreferences = Snabble.getInstance().getUserPreferences();

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
            Logger.errorEvent("Removing payment credentials, because key store id differs");
            removeInvalidCredentials();
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

    public void add(PaymentCredentials credentials) {
        if (credentials == null) {
            return;
        }

        ensureKeyStoreIsAccessible();

        data.credentialsList.add(credentials);
        save();
        notifyPaymentCredentialsAdded(credentials);
        notifyChanged();
    }

    public void remove(PaymentCredentials credentials) {
        if (data.credentialsList.remove(credentials)) {
            save();
            notifyChanged();
        }
    }

    public void removeInvalidCredentials() {
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
                    Logger.errorEvent(null, "Deleted payment credentials because device is not secure anymore. Lost access to %d payment credentials", removals.size());
                }
            }
        }
    }

    public void clear() {
        if (data.credentialsList.size() > 0) {
            data.credentialsList.clear();
            save();
            notifyChanged();
        }
    }

    void save() {
        Gson gson = new Gson();
        String json = gson.toJson(data);
        sharedPreferences.edit().putString(credentialsKey, json).apply();
    }

    private void load() {
        Gson gson = new Gson();
        data = new Data();
        data.credentialsList = new ArrayList<>();

        String json = sharedPreferences.getString(credentialsKey, null);
        if (json != null) {
            try {
                data = gson.fromJson(json, Data.class);
            } catch (Exception e) {
                Logger.errorEvent("Could not read payment credentials: %s", e.getMessage());
            }

            if (data.credentialsList == null) {
                data.credentialsList = new ArrayList<>();
            }

            validate();
        }
    }

    private void validate() {
        boolean changed = false;

        List<PaymentCredentials> removals = new ArrayList<>();

        for (PaymentCredentials credentials : data.credentialsList) {
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
            data.credentialsList.remove(credentials);
        }

        // old apps don't have ids set
        for (PaymentCredentials pc : data.credentialsList) {
            if (pc.getId() == null) {
                pc.generateId();
                changed = true;
            }
        }

        if (changed) {
            save();
        }
    }

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
                }
            }
        }

        if (project.getGooglePayHelper() != null
         && project.getGooglePayHelper().isGooglePayAvailable()) {
            count++;
        }

        return count;
    }

    public int getUsablePaymentCredentialsCount() {
        int i = 0;
        for (PaymentCredentials pc : data.credentialsList) {
            if (pc.getAppId().equals(Snabble.getInstance().getConfig().appId)) {
                i++;
            }
        }
        return i;
    }

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

    public void addCallback(Callback cb) {
        if (!callbacks.contains(cb)) {
            callbacks.add(cb);
        }
    }

    public void removeCallback(Callback cb) {
        callbacks.remove(cb);
    }
}
