package io.snabble.sdk.payment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.snabble.sdk.Environment;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.security.SecureStorageProvider;
import io.snabble.sdk.utils.security.SecureStorageProviderFactory;

public class PaymentCredentialsStore {
    private static final String SHARED_PREFERENCES_TAG = "snabble_payment";
    private static final String SHARED_PREFERENCES_CREDENTIALS = "credentials";
    private static final String ALIAS = "SnabblePaymentCredentialsStore";

    private SharedPreferences sharedPreferences;
    private List<PaymentCredentials> credentialsList = new ArrayList<>();
    private List<Callback> callbacks = new CopyOnWriteArrayList<>();
    private String credentialsKey;

    private SecureStorageProvider secureStorageProvider;

    public PaymentCredentialsStore(Context context, Environment environment) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);
        credentialsKey = SHARED_PREFERENCES_CREDENTIALS + "_" + (environment != null ? environment.name() : "_UNKNOWN");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            secureStorageProvider = SecureStorageProviderFactory.create(context, ALIAS, true);
        }

        loadFromLocalStore();
    }

    public List<PaymentCredentials> getUserPaymentCredentials() {
        return credentialsList;
    }

    public void add(PaymentCredentials credentials) {
        credentialsList.add(credentials);

        saveToLocalStore();
        notifyChanged();
    }

    public void remove(PaymentCredentials credentials) {
        credentialsList.remove(credentials);

        saveToLocalStore();
        notifyChanged();
    }

    private void saveToLocalStore() {
        Gson gson = new Gson();
        String json = gson.toJson(credentialsList);

        if (secureStorageProvider != null) {
            byte[] encrypted = secureStorageProvider.encrypt(json.getBytes());
            if (encrypted != null) {
                json = new String(encrypted);
            } else {
                throw new RuntimeException("WTF");
            }
        }

        sharedPreferences.edit().putString(credentialsKey, json).apply();
    }

    private void loadFromLocalStore() {
        Gson gson = new Gson();

        String json = sharedPreferences.getString(credentialsKey, null);
        if(json != null) {
            credentialsList = new ArrayList<>();

            try {
                if (secureStorageProvider != null) {
                    json = new String(secureStorageProvider.decrypt(json.getBytes()));
                }

                List<PaymentCredentials> list = gson.fromJson(json, new TypeToken<List<PaymentCredentials>>() {}.getType());
                for (PaymentCredentials credentials : list) {
                    if (credentials.validate()) {
                        credentialsList.add(credentials);
                    }
                }
            } catch (Exception e) {
                Logger.e("Could not read payment credentials: %s", e.getMessage());
            }

            saveToLocalStore();
        }
    }

    public void notifyChanged() {
        for(Callback cb : callbacks) {
            cb.onChanged();
        }
    }

    public void addCallback(Callback cb) {
        if(!callbacks.contains(cb)) {
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
