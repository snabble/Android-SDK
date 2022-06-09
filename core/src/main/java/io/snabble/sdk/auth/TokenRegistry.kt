package io.snabble.sdk.auth

import android.util.Base64
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble.instance
import okhttp3.OkHttpClient
import io.snabble.sdk.UserPreferences
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.lang.IllegalArgumentException
import java.util.HashMap

class TokenRegistry(
    private val okHttpClient: OkHttpClient,
    private val userPreferences: UserPreferences,
    private val appId: String?,
    secret: String?
) {
    private var totp: Totp? = null
    private val tokens: MutableMap<String, Token> = HashMap()
    private var timeOffset: Long = 0

    init {
        try {
            val secretData = Base32String.decode(secret)
            totp = Totp("HmacSHA256", secretData, 8, 30)
        } catch (e: Base32String.DecodingException) {
            e.printStackTrace()
        }
        userPreferences.addOnNewAppUserListener { invalidate() }
    }

    private fun invalidate() {
        tokens.clear()
    }

    @Synchronized
    private fun refreshToken(project: Project, isRetry: Boolean): Token? {
        if (totp == null) {
            return null
        }
        val time = offsetTime
        Logger.d("Getting token for %s, t=%s, c=%s", project.id, time.toString(), (time / 30).toString())
        val appUser = userPreferences.appUser
        val auth: String
        val url: String

        if (appUser != null) {
            auth = appId + ":" + totp?.generate(time) + ":" + appUser.id + ":" + appUser.secret
            url = requireNotNull(project.tokensUrl)
        } else {
            auth = appId + ":" + totp?.generate(time)
            url = requireNotNull(project.appUserUrl)
        }

        val base64 = try {
            Base64.encodeToString(auth.toByteArray(charset("UTF-8")), Base64.NO_WRAP)
        } catch (e: UnsupportedEncodingException) {
            // cant recover from this
            return null
        }

        var request = try {
            Request.Builder().url(url)
        } catch (e: IllegalArgumentException) {
            return null
        }

        request = if (appUser != null) {
            request.get()
        } else {
            request.post("".toRequestBody(null))
        }
        request.addHeader("Authorization", "Basic $base64")
            .addHeader("Client-ID", userPreferences.clientId.orEmpty())
            .build()
        var response: Response? = null
        try {
            response = okHttpClient.newCall(request.build()).execute()
            val body = response.body?.string()
            response.close()
            if (response.isSuccessful) {
                Logger.d("Successfully generated token for %s", project.id)
                adjustTimeOffset(response)
                return if (appUser == null) {
                    val (token, appUser1) = GsonHolder.get().fromJson(body, AppUserAndToken::class.java)
                    userPreferences.appUser = appUser1
                    tokens[project.id] = token
                    token
                } else {
                    val token = GsonHolder.get().fromJson(body, Token::class.java)
                    tokens[project.id] = token
                    token
                }
            } else {
                if (!isRetry) {
                    Logger.d("Could not generate token, trying again with server time")
                    adjustTimeOffset(response)
                    return refreshToken(project, true)
                } else {
                    Logger.e("Could not generate token: %s", body)
                }
            }
        } catch (e: IOException) {
            response?.close()
            Logger.e("Could not generate token: %s", e.toString())
        }
        return null
    }

    private fun adjustTimeOffset(response: Response) {
        val serverDate = response.headers.getDate("Date")
        if (serverDate != null) {
            timeOffset = serverDate.time - System.currentTimeMillis()
            Logger.d("timeOffset = %d", timeOffset)
        }
    }

    private val offsetTime: Long
        get() = (System.currentTimeMillis() + timeOffset) / 1000

    /**
     * Synchronously retrieves a token for the project.
     *
     * May do synchronous http requests, if the token is invalid.
     * If a valid token is available, it will be returned without doing http requests.
     *
     * Returns null if not valid token could be generated. (invalid secret, timeouts, no connection)
     */
    @Synchronized
    fun getToken(project: Project?): Token? {
        if (project == null) {
            return null
        }
        var token = tokens[project.id]
        if (token != null) {
            val tokenInterval = token.expiresAt - token.issuedAt
            val invalidAt = token.issuedAt + tokenInterval / 2
            val seconds = offsetTime
            if (seconds >= invalidAt) {
                Logger.d("Token timed out, requesting new token")
                val newToken = refreshToken(project, false)
                if (newToken != null) {
                    token = newToken
                }
                instance.users.update()
            }
        } else {
            token = refreshToken(project, false)
            instance.users.update()
        }
        return token
    }
}