package io.snabble.sdk.payment;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.snabble.sdk.Environment;
import io.snabble.sdk.Snabble;

public class PaymentCredentialsStore {
    private static final String SHARED_PREFERENCES_TAG = "snabble_payment";
    private static final String SHARED_PREFERENCES_CREDENTIALS = "credentials";

    private SharedPreferences sharedPreferences;
    private List<PaymentCredentials> credentialsList = new ArrayList<>();
    private List<Callback> callbacks = new CopyOnWriteArrayList<>();
    private String credentialsKey;

    public PaymentCredentialsStore(Context context, Environment environment) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);
        credentialsKey = SHARED_PREFERENCES_CREDENTIALS + "_" + environment.name();

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
        sharedPreferences.edit().putString(credentialsKey, json).apply();
    }

    private void loadFromLocalStore() {
        Gson gson = new Gson();

        String json = sharedPreferences.getString(credentialsKey, null);
        if(json != null){
            credentialsList = new ArrayList<>();

            List<PaymentCredentials> list = gson.fromJson(json, new TypeToken<List<PaymentCredentials>>(){}.getType());
            for (PaymentCredentials credentials : list) {
                if(credentials.validate()) {
                    credentialsList.add(credentials);
                }
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
