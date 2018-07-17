package io.snabble.sdk.payment;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaymentCredentialsStore {
    private static final String SHARED_PREFERENCES_TAG = "snabble_prefs";
    private static final String SHARED_PREFERENCES_SEPA = "sepa";

    private SharedPreferences sharedPreferences;
    public List<SEPACard> sepaCards = new ArrayList<>();

    public PaymentCredentialsStore(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);

        loadFromLocalStore();
    }

    public List<SEPACard> getSEPACards() {
        return Collections.unmodifiableList(sepaCards);
    }

    public void add(SEPACard sepaCard) {
        sepaCards.add(sepaCard);
    }

    public void remove(SEPACard sepaCard) {
        sepaCards.remove(sepaCard);
    }

    // TODO encryption

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
}
