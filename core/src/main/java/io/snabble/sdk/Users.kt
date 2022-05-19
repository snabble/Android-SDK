package io.snabble.sdk

import androidx.annotation.RestrictTo
import io.snabble.sdk.utils.SimpleJsonCallback
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.UserPreferences.ConsentStatus
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**
 * Class managing per-user settings of the backend
 */
class Users
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal constructor(userPreferences: UserPreferences) {
    interface UpdateUserCallback {
        fun success()
        fun failure()
    }

    interface GetUserCallback {
        fun success(response: Response)
        fun failure()
    }

    private class Request {
        var id: String? = null
        var dayOfBirth: String? = null
    }

    class Response {
        var id: String? = null
        var dayOfBirth: String? = null
        var bornBeforeOrOn: String? = null
    }

    private class UpdateConsentRequest {
        var version: String? = null
    }

    private var updateBirthdayCall: Call? = null
    private var postConsentCall: Call? = null
    private val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    private val userPreferences: UserPreferences

    init {
        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        this.userPreferences = userPreferences
    }

    /**
     * Gets the current information about the user from the backend.
     */
    operator fun get(callback: GetUserCallback) {
        val url = Snabble.usersUrl
        val appUser = Snabble.userPreferences.appUser
        if (appUser != null && url != null) {
            val request = Builder()
                .get()
                .url(url.replace("{appUserID}", appUser.id))
                .build()

            val okHttpClient = Snabble.projects[0].okHttpClient
            okHttpClient.newCall(request).enqueue(object : SimpleJsonCallback<Response>(
                Response::class.java
            ) {
                override fun success(response: Response) {
                    callback.success(response)
                }

                override fun error(t: Throwable) {
                    callback.failure()
                }
            })
        } else {
            callback.failure()
        }
    }

    /**
     * Sets the birthday of the user.
     */
    fun setBirthday(birthday: Date, updateUserCallback: UpdateUserCallback) {
        val url = Snabble.usersUrl
        val appUser = Snabble.userPreferences.appUser
        if (appUser != null && url != null) {
            val updateBirthdayRequest = Request()
            updateBirthdayRequest.id = appUser.id
            updateBirthdayRequest.dayOfBirth = simpleDateFormat.format(birthday)

            val requestBody = GsonHolder.get().toJson(updateBirthdayRequest)
                .toRequestBody("application/json".toMediaType())

            val request: okhttp3.Request = Builder()
                .patch(requestBody)
                .url(url.replace("{appUserID}", appUser.id))
                .build()
            val okHttpClient = Snabble.projects[0].okHttpClient

            updateBirthdayCall = okHttpClient.newCall(request)
            updateBirthdayCall?.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    updateBirthdayCall = null
                    updateUserCallback.failure()
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    if (response.isSuccessful) {
                        updateBirthdayCall = null
                        updateUserCallback.success()
                    } else {
                        updateBirthdayCall = null
                        updateUserCallback.failure()
                    }
                }
            })
        } else {
            updateUserCallback.failure()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun postPendingConsents() {
        val consentStatus = userPreferences.consentStatus
        if (consentStatus === ConsentStatus.TRANSMIT_FAILED) {
            postConsentVersion()
        } else if (consentStatus === ConsentStatus.TRANSMITTING) {
            if (postConsentCall == null) {
                postConsentVersion()
            }
        }
    }

    /**
     * Updates the given consent of a user
     *
     * @param version The last version the user accepted
     */
    fun setConsent(version: String?) {
        userPreferences.consentVersion = version
        postConsentVersion()
    }

    private fun postConsentVersion() {
        val url = Snabble.consentUrl
        val appUser = userPreferences.appUser
        if (appUser == null || url == null) {
            return
        }
        val version = userPreferences.consentVersion

        val updateBirthdayRequest = UpdateConsentRequest()
        updateBirthdayRequest.version = version
        val requestBody = GsonHolder.get().toJson(updateBirthdayRequest)
            .toRequestBody("application/json".toMediaType())

        val request: okhttp3.Request = Builder()
            .post(requestBody)
            .url(url.replace("{appUserID}", appUser.id))
            .build()

        userPreferences.consentStatus = ConsentStatus.TRANSMITTING

        val okHttpClient = Snabble.projects[0].okHttpClient
        postConsentCall = okHttpClient.newCall(request)
        postConsentCall?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                postConsentCall = null
                userPreferences.consentStatus = ConsentStatus.TRANSMIT_FAILED
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    postConsentCall = null
                    userPreferences.consentStatus = ConsentStatus.ACCEPTED
                } else {
                    postConsentCall = null
                    if (response.code == 400) {
                        userPreferences.consentStatus = ConsentStatus.ACCEPTED
                    } else {
                        userPreferences.consentStatus = ConsentStatus.TRANSMIT_FAILED
                    }
                }
            }
        })
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun update() {
        get(object : GetUserCallback {
            override fun success(response: Response) {
                var birthday: Date? = null

                val dayOfBirth = response.dayOfBirth
                val bornBeforeOrOn = response.bornBeforeOrOn

                try {
                    if (dayOfBirth != null) {
                        birthday = simpleDateFormat.parse(dayOfBirth)
                    } else if (bornBeforeOrOn != null) {
                        birthday = simpleDateFormat.parse(bornBeforeOrOn)
                    }
                } catch (ignored: Exception) { }

                Snabble.userPreferences.birthday = birthday
            }

            override fun failure() {}
        })
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun cancelUpdatingUser() {
        if (updateBirthdayCall != null) {
            updateBirthdayCall?.cancel()
        }
    }
}