package io.snabble.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import io.snabble.sdk.auth.AppUser;
import io.snabble.sdk.utils.Logger;

public class UserPreferences {
    enum ConsentStatus {
        UNDECIDED,
        TRANSMITTING,
        TRANSMIT_FAILED,
        ACCEPTED
    }

    private static final String SHARED_PREFERENCES_TAG = "snabble_prefs";
    private static final String SHARED_PREFERENCES_CLIENT_ID = "Client-ID";
    private static final String SHARED_PREFERENCES_APPUSER_ID = "AppUser-ID";
    private static final String SHARED_PREFERENCES_APPUSER_SECRET = "AppUser-Secret";
    private static final String SHARED_PREFERENCES_BIRTHDAY = "Birthday_v2";
    private static final String SHARED_PREFERENCES_CONSENT_STATUS = "ConsentStatus";
    private static final String SHARED_PREFERENCES_CONSENT_VERSION = "ConsentVersion";
    private static final String SHARED_PREFERENCES_USE_KEYGUARD = "useKeyguard";

    private static final SimpleDateFormat BIRTHDAY_FORMAT = new SimpleDateFormat("yyyy/MM/dd");

    private final SharedPreferences sharedPreferences;
    private final List<OnNewAppUserListener> onNewAppUserListeners;

    UserPreferences(Context context) {
        this.sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE);
        this.onNewAppUserListeners = new CopyOnWriteArrayList<>();

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

    private String getEnvironmentKey() {
        Environment environment = Snabble.getInstance().getEnvironment();
        if (environment != null) {
            return environment.name();
        } else {
            return "UNKNOWN";
        }
    }

    private String getAppUserIdKey() {
        Config config = Snabble.getInstance().getConfig();
        return SHARED_PREFERENCES_APPUSER_ID + "_" + getEnvironmentKey() + config.appId;
    }

    private String getAppUserIdSecret() {
        Config config = Snabble.getInstance().getConfig();
        return SHARED_PREFERENCES_APPUSER_SECRET + "_" + getEnvironmentKey() + config.appId;
    }

    public AppUser getAppUser() {
        Config config = Snabble.getInstance().getConfig();
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

            Logger.d("Clearing app user");
            notifyOnNewAppUser(null);
            return;
        }

        if (appUser.id != null && appUser.secret != null) {
            sharedPreferences.edit()
                    .putString(getAppUserIdKey(), appUser.id)
                    .putString(getAppUserIdSecret(), appUser.secret)
                    .apply();

            Logger.d("Setting app user to %s", appUser.id);
            notifyOnNewAppUser(appUser);
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
        if (appUserBase64 == null) {
            setAppUser(null);
            return;
        }

        String appUser = new String(Base64.decode(appUserBase64, Base64.DEFAULT));
        String[] split = appUser.split(":");

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

    public void setBirthday(Date date) {
        sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_BIRTHDAY, BIRTHDAY_FORMAT.format(date))
                .apply();
    }

    public Date getBirthday() {
        String s = sharedPreferences.getString(SHARED_PREFERENCES_BIRTHDAY, null);
        if (s == null) {
            return null;
        }

        try {
            return BIRTHDAY_FORMAT.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    public void setConsentStatus(ConsentStatus consent) {
        sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_CONSENT_STATUS, consent.name())
                .apply();
    }

    public ConsentStatus getConsentStatus() {
        String s = sharedPreferences.getString(SHARED_PREFERENCES_BIRTHDAY, null);
        if (s == null) {
            return ConsentStatus.UNDECIDED;
        }

        try {
            return ConsentStatus.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    public void setConsentVersion(String version) {
        sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_CONSENT_VERSION, version)
                .apply();
    }

    public String getConsentVersion() {
        return sharedPreferences.getString(SHARED_PREFERENCES_CONSENT_VERSION, null);
    }

    public void addOnNewAppUserListener(OnNewAppUserListener onNewAppUserListener) {
        if (!onNewAppUserListeners.contains(onNewAppUserListener)) {
            onNewAppUserListeners.add(onNewAppUserListener);
        }
    }

    public void removeOnNewAppUserListener(OnNewAppUserListener onNewAppUserListener) {
        onNewAppUserListeners.remove(onNewAppUserListener);
    }

    private void notifyOnNewAppUser(AppUser appUser) {
        for (OnNewAppUserListener listener : onNewAppUserListeners) {
            listener.onNewAppUser(appUser);
        }
    }

    public interface OnNewAppUserListener {
        void onNewAppUser(AppUser appUser);
    }
}