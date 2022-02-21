package io.snabble.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import io.snabble.sdk.auth.AppUser
import io.snabble.sdk.utils.Logger
import java.io.UnsupportedEncodingException
import java.lang.Exception
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class UserPreferences internal constructor(context: Context) {
    enum class ConsentStatus {
        UNDECIDED,
        TRANSMITTING,
        TRANSMIT_FAILED,
        ACCEPTED
    }

    companion object {
        private const val SHARED_PREFERENCES_TAG = "snabble_prefs"
        private const val SHARED_PREFERENCES_CLIENT_ID = "Client-ID"
        private const val SHARED_PREFERENCES_APPUSER_ID = "AppUser-ID"
        private const val SHARED_PREFERENCES_APPUSER_SECRET = "AppUser-Secret"
        private const val SHARED_PREFERENCES_BIRTHDAY = "Birthday_v2"
        private const val SHARED_PREFERENCES_CONSENT_STATUS = "ConsentStatus"
        private const val SHARED_PREFERENCES_CONSENT_VERSION = "ConsentVersion"
        private const val SHARED_PREFERENCES_USE_KEYGUARD = "useKeyguard"
        private const val SHARED_PREFERENCES_LAST_CHECKED_IN_SHOP = "lastShop"

        @SuppressLint("SimpleDateFormat")
        private val BIRTHDAY_FORMAT = SimpleDateFormat("yyyy/MM/dd")
    }

    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_TAG, Context.MODE_PRIVATE)
    private val onNewAppUserListeners: MutableList<OnNewAppUserListener> = CopyOnWriteArrayList()

    init {
        if (clientId == null) {
            generateClientId()
        }
    }

    private fun generateClientId() {
        clientId = UUID.randomUUID().toString()
            .replace("-", "")
            .lowercase(Locale.ROOT)
    }

    private val environmentKey: String
        get() {
            val environment = Snabble.environment
            return environment?.name ?: "UNKNOWN"
        }

    private val appUserIdKey: String
        get() {
            val appId = Snabble.config.appId
            return "${SHARED_PREFERENCES_APPUSER_ID}_$environmentKey$appId"
        }

    private val appUserIdSecret: String
        get() {
            val appId = Snabble.config.appId
            return "${SHARED_PREFERENCES_APPUSER_SECRET}_$environmentKey$appId"
        }

    var appUser: AppUser?
        get() {
            val appUserId = sharedPreferences.getString(appUserIdKey, null)
            val appUserSecret = sharedPreferences.getString(appUserIdSecret, null)

            return if (appUserId != null && appUserSecret != null) {
                AppUser(appUserId, appUserSecret)
            } else {
                null
            }
        }
        set(appUser) {
            if (appUser == null) {
                sharedPreferences.edit()
                    .putString(appUserIdKey, null)
                    .putString(appUserIdSecret, null)
                    .apply()
                Logger.d("Clearing app user")
                notifyOnNewAppUser(null)
                return
            }
            if (appUser.id != null && appUser.secret != null) {
                sharedPreferences.edit()
                    .putString(appUserIdKey, appUser.id)
                    .putString(appUserIdSecret, appUser.secret)
                    .apply()
                Logger.d("Setting app user to %s", appUser.id)
                notifyOnNewAppUser(appUser)
            }
        }

    var appUserBase64: String?
        get() {
            val appUser = appUser
            if (appUser != null) {
                val content = appUser.id + ":" + appUser.secret
                return try {
                    Base64.encodeToString(content.toByteArray(charset("UTF-8")), Base64.NO_WRAP)
                } catch (e: UnsupportedEncodingException) {
                    null
                }
            }
            return null
        }
        set(appUserBase64) {
            if (appUserBase64 == null) {
                appUser = null
                return
            }
            var appUserString = String(Base64.decode(appUserBase64, Base64.DEFAULT))
            val split = appUserString.split(":")
            if (split.size == 2) {
                val appUserId = split[0]
                val appUserSecret = split[1]
                if (appUserId.length > 0 && appUserSecret.length > 0) {
                    appUser = AppUser(appUserId, appUserSecret)
                }
            }
        }

    var clientId: String?
        get() =
            sharedPreferences.getString(SHARED_PREFERENCES_CLIENT_ID, null)
        set(clientId) {
            sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_CLIENT_ID, clientId)
                .apply()
        }

    var lastCheckedInShopId: String?
        get() =
            sharedPreferences.getString(SHARED_PREFERENCES_LAST_CHECKED_IN_SHOP, null)
        set(shopId) {
            sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_LAST_CHECKED_IN_SHOP, shopId)
                .apply()
        }

    var birthday: Date?
        get() {
            val s = sharedPreferences.getString(SHARED_PREFERENCES_BIRTHDAY, null)
                ?: return null
            return try {
                BIRTHDAY_FORMAT.parse(s)
            } catch (e: ParseException) {
                null
            }
        }
        set(date) {
            sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_BIRTHDAY, date?.let { BIRTHDAY_FORMAT.format(date) })
                .apply()
        }

    var consentStatus: ConsentStatus
        get() {
            val s = sharedPreferences.getString(SHARED_PREFERENCES_BIRTHDAY, null)
                ?: return ConsentStatus.UNDECIDED
            return try {
                ConsentStatus.valueOf(s)
            } catch (e: Exception) {
                return ConsentStatus.UNDECIDED
            }
        }
        set(consent) =
            sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_CONSENT_STATUS, consent.name)
                .apply()

    var consentVersion: String?
        get() = sharedPreferences.getString(SHARED_PREFERENCES_CONSENT_VERSION, null)
        set(version) {
            sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_CONSENT_VERSION, version)
                .apply()
        }

    fun addOnNewAppUserListener(onNewAppUserListener: OnNewAppUserListener) {
        if (!onNewAppUserListeners.contains(onNewAppUserListener)) {
            onNewAppUserListeners.add(onNewAppUserListener)
        }
    }

    fun removeOnNewAppUserListener(onNewAppUserListener: OnNewAppUserListener) {
        onNewAppUserListeners.remove(onNewAppUserListener)
    }

    private fun notifyOnNewAppUser(appUser: AppUser?) {
        for (listener in onNewAppUserListeners) {
            listener.onNewAppUser(appUser)
        }
    }

    fun interface OnNewAppUserListener {
        fun onNewAppUser(appUser: AppUser?)
    }
}