package io.snabble.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import androidx.annotation.RestrictTo
import io.snabble.sdk.auth.AppUser
import io.snabble.sdk.utils.Logger
import java.io.UnsupportedEncodingException
import java.lang.Exception
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Class managing local user preferences
 */
class UserPreferences internal constructor(context: Context) {
    /**
     * Enum describing the current status of given user consent
     */
    enum class ConsentStatus {
        /** No consent given **/
        UNDECIDED,
        /** The consent is currently transferred to the backend **/
        TRANSMITTING,
        /** The consent could not be transferred to the backend **/
        TRANSMIT_FAILED,
        /** The consent was successfully transferred to the backend **/
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
        private const val SHARED_PREFERENCES_LAST_CHECKED_IN_SHOP = "lastShop"
        private const val SHARED_PREFERENCES_BASE_URL = "base_url"

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
        /** Gets the current id, secret pair of the locally generated app user */
        get() {
            val appUserId = sharedPreferences.getString(appUserIdKey, null)
            val appUserSecret = sharedPreferences.getString(appUserIdSecret, null)

            return if (appUserId != null && appUserSecret != null) {
                AppUser(appUserId, appUserSecret)
            } else {
                null
            }
        }
        /** Sets a new app user */
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
        /**
         * Gets a base64 string, which is the app user concatenated by ':'
         *
         * Can be used to store in other user databases to be able to restore the snabble sdk app user
         */
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
        /**
         * Sets a base64 string, which is the app user concatenated by ':'
         *
         * Can be used to restore the app user
         */
        set(appUserBase64) {
            if (appUserBase64 == null) {
                appUser = null
                return
            }
            val appUserString = String(Base64.decode(appUserBase64, Base64.DEFAULT))
            val split = appUserString.split(":")
            if (split.size == 2) {
                val appUserId = split[0]
                val appUserSecret = split[1]
                if (appUserId.isNotEmpty() && appUserSecret.isNotEmpty()) {
                    appUser = AppUser(appUserId, appUserSecret)
                }
            }
        }

    /**
     * Client id of the snabble SDK, which gets used in every request to our backend
     * to identify different devices.
     *
     * Does not contain any personalized information.
     */
    var clientId: String?
        get() = sharedPreferences.getString(SHARED_PREFERENCES_CLIENT_ID, null)
        set(clientId) {
            sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_CLIENT_ID, clientId)
                .apply()
        }

    var environment: Environment
        get() = Environment.getEnvironmentByUrl(baseUrl)
        set(value) {
            baseUrl = value.baseUrl
        }

    private var baseUrl: String
        get() = sharedPreferences.getString(SHARED_PREFERENCES_BASE_URL, null) ?: Environment.PRODUCTION.baseUrl
        set(value) {
            sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_BASE_URL, value)
                .apply()
        }

    internal var lastCheckedInShopId: String?
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        get() =
            sharedPreferences.getString(SHARED_PREFERENCES_LAST_CHECKED_IN_SHOP, null)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        set(shopId) {
            sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_LAST_CHECKED_IN_SHOP, shopId)
                .apply()
        }

    var birthday: Date?
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        get() {
            val s = sharedPreferences.getString(SHARED_PREFERENCES_BIRTHDAY, null)
                ?: return null
            return try {
                BIRTHDAY_FORMAT.parse(s)
            } catch (e: ParseException) {
                null
            }
        }
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        set(date) {
            sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_BIRTHDAY, date?.let { BIRTHDAY_FORMAT.format(date) })
                .apply()
        }

    /**
     * The current consent status
     */
    var consentStatus: ConsentStatus
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        get() {
            val s = sharedPreferences.getString(SHARED_PREFERENCES_BIRTHDAY, null)
                ?: return ConsentStatus.UNDECIDED
            return try {
                ConsentStatus.valueOf(s)
            } catch (e: Exception) {
                return ConsentStatus.UNDECIDED
            }
        }
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        set(consent) =
            sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_CONSENT_STATUS, consent.name)
                .apply()

    /**
     * The current accepted consent version
     */
    var consentVersion: String?
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        get() = sharedPreferences.getString(SHARED_PREFERENCES_CONSENT_VERSION, null)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        set(version) {
            sharedPreferences.edit()
                .putString(SHARED_PREFERENCES_CONSENT_VERSION, version)
                .apply()
        }

    /**
     * Adds a listener that gets notified when a new app user is generated
     */
    fun addOnNewAppUserListener(onNewAppUserListener: OnNewAppUserListener) {
        if (!onNewAppUserListeners.contains(onNewAppUserListener)) {
            onNewAppUserListeners.add(onNewAppUserListener)
        }
    }

    /**
     * Removed a previously added listener
     */
    fun removeOnNewAppUserListener(onNewAppUserListener: OnNewAppUserListener) {
        onNewAppUserListeners.remove(onNewAppUserListener)
    }

    private fun notifyOnNewAppUser(appUser: AppUser?) {
        for (listener in onNewAppUserListeners) {
            listener.onNewAppUser(appUser)
        }
    }

    /**
     * Interface that gets called when a new app user is generated
     */
    fun interface OnNewAppUserListener {
        /**
         * Gets called when a new app user is generated
         */
        fun onNewAppUser(appUser: AppUser?)
    }
}