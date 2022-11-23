package io.snabble.sdk.widgets.snabble.devsettings.repositories

import android.content.SharedPreferences
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success
import kotlinx.coroutines.flow.MutableStateFlow

interface DevSettingsRepository {

    val devSettingsEnabled: MutableStateFlow<Boolean>

    suspend fun enableDevSettings(password: String): Result
}

internal class DevSettingsRepositoryImpl(
    private val sharedPreferences: SharedPreferences,
) : DevSettingsRepository {

    override val devSettingsEnabled: MutableStateFlow<Boolean>
        get() = MutableStateFlow(sharedPreferences.getBoolean(DEV_SETTINGS_KEY, false))

    override suspend fun enableDevSettings(password: String): Result {
        // TODO: Change to right password
        return if (password == "Test") {
            sharedPreferences.edit().putBoolean(DEV_SETTINGS_KEY, true).apply()
            devSettingsEnabled.tryEmit(true)
            Success("DevSettings enabled")
        } else {
            Error("Failed to enable DevSettings")
        }
    }
}

private const val DEV_SETTINGS_KEY = "DevSettings"
