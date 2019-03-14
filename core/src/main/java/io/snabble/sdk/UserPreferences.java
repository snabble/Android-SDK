package io.snabble.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

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

    /**
     * Enables keyguard authentication for online payment using user credentials.
     * Requires the user to have a PIN, Fingerprint or other security locks.
     *
     * Payment credentials will then be securely stored using a generated key
     * using the Android KeyStore API.
     *
     * Removing the secure lock will cause the keys to be wiped and rendering the
     * stored payment credentials useless.
     *
     * Only supported for Android >= 4.3. For earlier Android versions the credentials will only
     * be stored using asynchronous RSA.
     *
     */
    public void setRequireKeyguardAuthenticationForPayment(boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            sharedPreferences.edit().putBoolean(SHARED_PREFERENCES_USE_KEYGUARD, enabled).apply();
        }
    }

    public boolean isRequiringKeyguardAuthenticationForPayment() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return sharedPreferences.getBoolean(SHARED_PREFERENCES_USE_KEYGUARD, false);
        }

        return false;
    }
}
