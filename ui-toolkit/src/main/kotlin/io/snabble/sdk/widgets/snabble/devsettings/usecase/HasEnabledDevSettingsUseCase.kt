package io.snabble.sdk.widgets.snabble.devsettings.usecase

import android.content.SharedPreferences
import io.snabble.sdk.widgets.snabble.devsettings.repositories.DevSettingsRepositoryImpl.Companion.DEV_SETTINGS_KEY
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface HasEnableDevSettingsUseCase {

    operator fun invoke(): Flow<Boolean>
}

class HasEnableDevSettingsUseCaseImpl(
    private val sharedPreferences: SharedPreferences,
) : HasEnableDevSettingsUseCase {

    override fun invoke(): Flow<Boolean> = callbackFlow {
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
}
