package io.snabble.sdk.widgets.snabble.devsettings.login.usecase

import android.content.SharedPreferences
import io.snabble.sdk.widgets.snabble.devsettings.login.repositories.DevSettingsLoginRepositoryImpl.Companion.KEY_ARE_DEV_SETTINGS_ENABLED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal interface HasEnabledDevSettingsUseCase {

    operator fun invoke(): Flow<Boolean>
}

internal class HasEnabledDevSettingsUseCaseImpl(
    private val sharedPreferences: SharedPreferences,
) : HasEnabledDevSettingsUseCase {

    override fun invoke(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (changedKey == KEY_ARE_DEV_SETTINGS_ENABLED) {
                trySend(prefs.getBoolean(KEY_ARE_DEV_SETTINGS_ENABLED, false))
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        trySend(sharedPreferences.getBoolean(KEY_ARE_DEV_SETTINGS_ENABLED, false))

        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}
