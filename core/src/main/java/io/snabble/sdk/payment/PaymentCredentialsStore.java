package io.snabble.sdk.payment;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.snabble.sdk.Environment;
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
        private boolean isKeyguarded;
    }

    private SharedPreferences sharedPreferences;
    private Data data;
    private List<Callback> callbacks = new CopyOnWriteArrayList<>();
    private String credentialsKey;
    private UserPreferences userPreferences;

    KeyStoreCipher keyStoreCipher;

    public PaymentCredentialsStore(Context context, Environment environment) {
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
            Snabble snabble = Snabble.getInstance();

            // KeyStore is not available on Android < 4.3
            if (userPreferences.isRequiringKeyguardAuthenticationForPayment()) {
                keyStoreCipher = KeyStoreCipher.create(snabble.getApplication(), "SnabblePaymentCredentialsStore", true);
            }
        }
    }

    private void ensureKeyStoreIsAccessible() {
        initializeKeyStore();

        if (keyStoreCipher != null) {
            keyStoreCipher.validate();

            String id = keyStoreCipher.id();
            if (id == null) {
                keyStoreCipher = null;
                return;
            }

            if (!id.equals(data.id)) {
                data.id = keyStoreCipher.id();
                data.isKeyguarded = true;
                clear();
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

        if (!secure || data.isKeyguarded != userPreferences.isRequiringKeyguardAuthenticationForPayment()) {
            Snabble snabble = Snabble.getInstance();
            if (snabble.getProjects().size() > 0) {
                if (data.credentialsList != null && data.credentialsList.size() > 0) {
                    snabble.getProjects().get(0).getEvents()
                            .log("Deleted payment credentials key store because device is not secure anymore. Lost access to %d payment credentials", data.credentialsList.size());
                }
            }

            generateRandomId();
            clear();
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

    public String id() {
        ensureKeyStoreIsAccessible();
        return data.id;
    }

    public void add(PaymentCredentials credentials) {
        ensureKeyStoreIsAccessible();

        data.credentialsList.add(credentials);
        save();
        notifyChanged();
    }

    public void remove(PaymentCredentials credentials) {
        if (data.credentialsList.remove(credentials)) {
            save();
            notifyChanged();
        }
    }

    public void clear() {
        if (data.credentialsList.size() > 0) {
            data.credentialsList.clear();
            save();
            notifyChanged();
        }
    }

    private void save() {
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
                Logger.e("Could not read payment credentials: %s", e.getMessage());
            }

            if (data.credentialsList == null) {
                data.credentialsList = new ArrayList<>();
            }

            validate();
        }
    }

    private void validate() {
        boolean changed = false;

        for (int i = data.credentialsList.size() - 1; i >= 0; i--) {
            PaymentCredentials credentials = data.credentialsList.get(i);

            if (!credentials.validate()) {
                data.credentialsList.remove(credentials);
                changed = true;
            } else {
                // app id's were not stored in old versions, if its not there assume the
                // current app id was the app id in which the payment method was created
                if (credentials.checkAppId()) {
                    changed = true;
                }
            }
        }

        if (changed) {
            save();
        }
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

    public interface Callback {
        void onChanged();
    }
}
