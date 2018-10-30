package io.snabble.sdk.payment;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PaymentCredentialsStore {
    private static final String SHARED_PREFERENCES_TAG = "snabble_payment";
    private static final String SHARED_PREFERENCES_CREDENTIALS = "credentials";

    private SharedPreferences sharedPreferences;
    private List<PaymentCredentials> credentialsList = new ArrayList<>();
    private List<Callback> callbacks = new CopyOnWriteArrayList<>();

    public PaymentCredentialsStore(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);

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
        sharedPreferences.edit().putString(SHARED_PREFERENCES_CREDENTIALS, json).apply();
    }

    private void loadFromLocalStore() {
        Gson gson = new Gson();

        String json = sharedPreferences.getString(SHARED_PREFERENCES_CREDENTIALS, null);
        if(json != null){
            credentialsList = gson.fromJson(json, new TypeToken<List<PaymentCredentials>>(){}.getType());
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
