package io.snabble.sdk.widgets.snabble.devsettings.repositories

import android.content.SharedPreferences
import android.util.Base64.NO_WRAP
import android.util.Base64.encodeToString
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success

interface DevSettingsRepository {

    suspend fun enableDevSettings(password: String): Result
}

internal class DevSettingsRepositoryImpl(
    private val sharedPreferences: SharedPreferences,
    private val devSettingPassword: String,
) : DevSettingsRepository {

    override suspend fun enableDevSettings(password: String): Result {
        val encodedPassword = encodeToString(password.toByteArray(charset("UTF-8")), NO_WRAP)
        return if (encodedPassword == devSettingPassword) {
            sharedPreferences.edit().putBoolean(DEV_SETTINGS_KEY, true).apply()
            Success("DevSettings enabled")
        } else {
            Error("Failed to enable DevSettings")
        }
    }

    companion object {

        const val DEV_SETTINGS_KEY = "DevSettings"
    }
}
