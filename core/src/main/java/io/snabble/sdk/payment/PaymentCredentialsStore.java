package io.snabble.sdk.payment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.snabble.sdk.Environment;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.security.KeyStoreCipher;

public class PaymentCredentialsStore {
    private SharedPreferences sharedPreferences;
    private List<PaymentCredentials> credentialsList = new ArrayList<>();
    private List<Callback> callbacks = new CopyOnWriteArrayList<>();
    private String credentialsKey;
    private boolean hadUnrecoverableCredentials;

    KeyStoreCipher keyStoreCipher;

    @SuppressLint("NewApi")
    public PaymentCredentialsStore(Context context, Environment environment) {
        sharedPreferences = context.getSharedPreferences("snabble_payment", Context.MODE_PRIVATE);
        credentialsKey = "credentials_" + (environment != null ? environment.name() : "_UNKNOWN");

        // KeyStore is not available on Android < 4.3
        if (Snabble.getInstance().getUserPreferences().isRequiringKeyguardAuthenticationForPayment()) {
            keyStoreCipher = KeyStoreCipher.create(context, "SnabblePaymentCredentialsStore", true);
        }

        load();

        hadUnrecoverableCredentials = credentialsList.size() > 0 && keyStoreCipher.size() == 0;
        if (hadUnrecoverableCredentials) {
            credentialsList.clear();
        }
    }

    public List<PaymentCredentials> getAll() {
        return Collections.unmodifiableList(credentialsList);
    }

    public boolean hadUnrecoverableCredentials() {
        return hadUnrecoverableCredentials;
    }

    public int size() {
        return credentialsList.size();
    }

    public void add(PaymentCredentials credentials) {
        credentialsList.add(credentials);
        save();
        notifyChanged();
    }

    public void remove(PaymentCredentials credentials) {
        credentialsList.remove(credentials);
        save();
        notifyChanged();
    }

    public void invalidate() {
        hadUnrecoverableCredentials = false;
        credentialsList.clear();
        save();
        notifyChanged();
    }

    private void save() {
        Gson gson = new Gson();
        String json = gson.toJson(credentialsList);
        sharedPreferences.edit().putString(credentialsKey, json).apply();
    }

    private void load() {
        Gson gson = new Gson();

        String json = sharedPreferences.getString(credentialsKey, null);
        if(json != null) {
            credentialsList = new ArrayList<>();

            try {
                List<PaymentCredentials> list = gson.fromJson(json, new TypeToken<List<PaymentCredentials>>() {}.getType());
                for (PaymentCredentials credentials : list) {
                    if (credentials.validate()) {
                        credentialsList.add(credentials);
                    }
                }
            } catch (Exception e) {
                Logger.e("Could not read payment credentials: %s", e.getMessage());
            }

            save();
        }
    }

    private void notifyChanged() {
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
