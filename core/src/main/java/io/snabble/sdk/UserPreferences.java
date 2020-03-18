package io.snabble.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.UUID;

import io.snabble.sdk.auth.AppUser;

public class UserPreferences {
    private static final String SHARED_PREFERENCES_TAG = "snabble_prefs";
    private static final String SHARED_PREFERENCES_CLIENT_ID = "Client-ID";
    private static final String SHARED_PREFERENCES_APPUSER_ID = "AppUser-ID";
    private static final String SHARED_PREFERENCES_APPUSER_SECRET = "AppUser-Secret";
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

        setClientId(clientId);
    }

    private String getAppUserIdKey() {
        Snabble.Config config = Snabble.getInstance().getConfig();
        return SHARED_PREFERENCES_APPUSER_ID + "_" + Snabble.getInstance().getEnvironment().name() + config.appId;
    }

    private String getAppUserIdSecret() {
        Snabble.Config config = Snabble.getInstance().getConfig();
        return SHARED_PREFERENCES_APPUSER_SECRET + "_" + Snabble.getInstance().getEnvironment().name() + config.appId;
    }

    public AppUser getAppUser() {
        Snabble.Config config = Snabble.getInstance().getConfig();
        if (config == null) {
            return null;
        }

        String appUserId = sharedPreferences.getString(getAppUserIdKey(), null);
        String appUserSecret = sharedPreferences.getString(getAppUserIdSecret(), null);

        if (appUserId != null && appUserSecret != null) {
            return new AppUser(appUserId, appUserSecret);
        } else {
            return null;
        }
    }

    public void setAppUser(AppUser appUser) {
        if (appUser == null) {
            sharedPreferences.edit()
                    .putString(getAppUserIdKey(), null)
                    .putString(getAppUserIdSecret(), null)
                    .apply();
            return;
        }

        if (appUser.id != null && appUser.secret != null) {
            sharedPreferences.edit()
                    .putString(getAppUserIdKey(), appUser.id)
                    .putString(getAppUserIdSecret(), appUser.secret)
                    .apply();
        }
    }

    public String getAppUserBase64() {
        AppUser appUser = getAppUser();
        if (appUser != null) {
            String content = appUser.id + ":" + appUser.secret;

            try {
                return Base64.encodeToString(content.getBytes("UTF-8"), Base64.NO_WRAP);
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }

        return null;
    }

    public void setAppUserBase64(String appUserBase64) {
        String[] split = appUserBase64.split(":");

        if (split.length == 2) {
            String appUserId = split[0];
            String appUserSecret = split[1];

            if (appUserId.length() > 0 && appUserSecret.length() > 0) {
                setAppUser(new AppUser(appUserId, appUserSecret));
            }
        }
    }

    private void setClientId(String clientId) {
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
