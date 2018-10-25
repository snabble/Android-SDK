package io.snabble.sdk.payment;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PaymentCredentialsStore {
    private static final String SHARED_PREFERENCES_TAG = "snabble_prefs";
    private static final String SHARED_PREFERENCES_SEPA = "sepa";

    private SharedPreferences sharedPreferences;
    private List<SEPAPaymentCredentials> sepaPaymentCredentials = new ArrayList<>();
    private List<Callback> callbacks = new CopyOnWriteArrayList<>();

    public PaymentCredentialsStore(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);

        loadFromLocalStore();
    }

    public List<PaymentCredentials> getUserPaymentMethods() {
        List<PaymentCredentials> paymentCredentials = new ArrayList<>();
        paymentCredentials.addAll(sepaPaymentCredentials);
        return paymentCredentials;
    }

    public void add(PaymentCredentials sepaCard) {
        if (sepaCard instanceof SEPAPaymentCredentials) {
            sepaPaymentCredentials.add((SEPAPaymentCredentials)sepaCard);
        }

        saveToLocalStore();
        notifyChanged();
    }

    public void remove(PaymentCredentials sepaCard) {
        if (sepaCard instanceof SEPAPaymentCredentials) {
            sepaPaymentCredentials.remove(sepaCard);
        }

        saveToLocalStore();
        notifyChanged();
    }

    private void saveToLocalStore() {
        Gson gson = new Gson();
        String json = gson.toJson(sepaPaymentCredentials);
        sharedPreferences.edit().putString(SHARED_PREFERENCES_SEPA, json).apply();
    }

    private void loadFromLocalStore() {
        Gson gson = new Gson();

        String json = sharedPreferences.getString(SHARED_PREFERENCES_SEPA, null);
        if(json != null){
            sepaPaymentCredentials = gson.fromJson(json, new TypeToken<List<SEPAPaymentCredentials>>(){}.getType());

            if(sepaPaymentCredentials == null) {
                sepaPaymentCredentials = new ArrayList<>();
            }
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
