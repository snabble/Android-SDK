package io.snabble.sdk.payment;

import android.content.Context;
import android.content.SharedPreferences;
import android.telecom.Call;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.snabble.sdk.UserPreferences;

public class PaymentCredentialsStore {
    private static final String SHARED_PREFERENCES_TAG = "snabble_prefs";
    private static final String SHARED_PREFERENCES_SEPA = "sepa";

    private SharedPreferences sharedPreferences;
    public List<SEPACard> sepaCards = new ArrayList<>();

    private List<Callback> callbacks = new CopyOnWriteArrayList<>();

    public PaymentCredentialsStore(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);

        loadFromLocalStore();
    }

    public List<UserPaymentMethod> getUserPaymentMethods() {
        List<UserPaymentMethod> userPaymentMethods = new ArrayList<>();
        userPaymentMethods.addAll(sepaCards);
        return userPaymentMethods;
    }

    public void add(UserPaymentMethod sepaCard) {
        if (sepaCard instanceof SEPACard) {
            sepaCards.add((SEPACard)sepaCard);
        }

        saveToLocalStore();
        notifyChanged();
    }

    public void remove(UserPaymentMethod sepaCard) {
        if (sepaCard instanceof SEPACard) {
            sepaCards.remove(sepaCard);
        }

        notifyChanged();
    }

    private void saveToLocalStore() {
        Gson gson = new Gson();
        String json = gson.toJson(sepaCards);
        sharedPreferences.edit().putString(SHARED_PREFERENCES_SEPA, json).apply();
    }

    private void loadFromLocalStore() {
        Gson gson = new Gson();

        String json = sharedPreferences.getString(SHARED_PREFERENCES_SEPA, null);
        if(json != null){
            sepaCards = gson.fromJson(json, new TypeToken<List<SEPACard>>(){}.getType());

            if(sepaCards == null) {
                sepaCards = new ArrayList<>();
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
