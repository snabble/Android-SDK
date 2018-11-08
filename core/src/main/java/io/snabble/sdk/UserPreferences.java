package io.snabble.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;
import java.util.UUID;

public class UserPreferences {
    private static final String SHARED_PREFERENCES_TAG = "snabble_prefs";
    private static final String SHARED_PREFERENCES_CLIENT_ID = "Client-ID";
    private static final String SHARED_PREFERENCES_USE_KEYGUARD = "useKeyguard";

    private SharedPreferences sharedPreferences;

    UserPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);

        if (getClientId() == null) {
            generateClientId();
        }
    }

    private void generateClientId() {
        String clientId = UUID.randomUUID().toString()
                .replace("-", "")
                .toLowerCase(Locale.ROOT);

        sharedPreferences.edit().putString(SHARED_PREFERENCES_CLIENT_ID, clientId).apply();
    }

    public String getClientId() {
        return sharedPreferences.getString(SHARED_PREFERENCES_CLIENT_ID, null);
    }

    public void setRequireKeyguardAuthenticationForPayment(boolean enabled) {
        sharedPreferences.edit().putBoolean(SHARED_PREFERENCES_USE_KEYGUARD, enabled).apply();
    }

    public boolean isRequiringKeyguardAuthenticationForPayment() {
        return sharedPreferences.getBoolean(SHARED_PREFERENCES_USE_KEYGUARD, false);
    }
}
