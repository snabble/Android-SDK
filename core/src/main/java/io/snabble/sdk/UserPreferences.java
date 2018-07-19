package io.snabble.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import io.snabble.sdk.payment.PaymentCredentialsStore;

import java.util.Locale;
import java.util.UUID;

public class UserPreferences {
    private static final String SHARED_PREFERENCES_TAG = "snabble_prefs";
    private static final String SHARED_PREFERENCES_CLIENT_ID = "Client-ID";

    private SharedPreferences sharedPreferences;
    private PaymentCredentialsStore paymentCredentialsStore;

    UserPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);

        if(getClientId() == null){
            generateClientId();
        }

        paymentCredentialsStore = new PaymentCredentialsStore(context);
    }

    private void generateClientId() {
        String clientId = UUID.randomUUID().toString()
                .replace("-", "")
                .toLowerCase(Locale.ROOT);

        sharedPreferences.edit().putString(SHARED_PREFERENCES_CLIENT_ID, clientId).apply();
    }

    public PaymentCredentialsStore getPaymentCredentialsStore() {
        return paymentCredentialsStore;
    }

    public String getClientId(){
        return sharedPreferences.getString(SHARED_PREFERENCES_CLIENT_ID, null);
    }
}
