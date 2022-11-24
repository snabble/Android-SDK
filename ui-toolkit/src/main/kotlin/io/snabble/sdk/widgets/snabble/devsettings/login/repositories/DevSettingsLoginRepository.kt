package io.snabble.sdk.widgets.snabble.devsettings.login.repositories

import android.content.SharedPreferences
import android.util.Base64.NO_WRAP
import android.util.Base64.encodeToString
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success

interface DevSettingsLoginRepository {

    suspend fun enableDevSettings(password: String): Result
}

internal class DevSettingsLoginRepositoryImpl(
    private val sharedPreferences: SharedPreferences,
    private val devSettingPassword: String,
) : DevSettingsLoginRepository {

    override suspend fun enableDevSettings(password: String): Result {
        val encodedPassword = encodeToString(password.toByteArray(charset("UTF-8")), NO_WRAP)
        return if (encodedPassword == devSettingPassword) {
            sharedPreferences.edit().putBoolean(KEY_ARE_DEV_SETTINGS_ENABLED, true).apply()
            Success("DevSettings enabled")
        } else {
            Error("Failed to enable DevSettings")
        }
    }

    companion object {

        const val KEY_ARE_DEV_SETTINGS_ENABLED = "AreDevSettingsEnabled"
    }
}
