package io.snabble.sdk.widgets.snabble.devsettings.repositories

import android.content.SharedPreferences
import android.util.Base64.NO_WRAP
import android.util.Base64.encodeToString
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface DevSettingsRepository {

    val devSettingsEnabled: Flow<Boolean>

    suspend fun enableDevSettings(password: String): Result
}

internal class DevSettingsRepositoryImpl(
    private val sharedPreferences: SharedPreferences,
    private val devSettingPassword: String,
) : DevSettingsRepository {

    override val devSettingsEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (changedKey == DEV_SETTINGS_KEY) {
                trySend(prefs.getBoolean(DEV_SETTINGS_KEY, false))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        trySend(sharedPreferences.getBoolean(DEV_SETTINGS_KEY, false))

        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    override suspend fun enableDevSettings(password: String): Result {
        val encodedPassword = encodeToString(password.toByteArray(charset("UTF-8")), NO_WRAP)
        return if (encodedPassword == devSettingPassword) {
            sharedPreferences.edit().putBoolean(DEV_SETTINGS_KEY, true).apply()
            Success("DevSettings enabled")
        } else {
            Error("Failed to enable DevSettings")
        }
    }
}

private const val DEV_SETTINGS_KEY = "DevSettings"
