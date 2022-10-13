package io.snabble.sdk.widgets.snabble.toggle.repository

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal interface ToggleRepository {

    suspend fun saveToggleState(key: String, isChecked: Boolean)

    suspend fun getToggleState(key: String): Flow<Boolean>
}

internal class ToggleRepositoryImpl(private val sharedPrefs: SharedPreferences) : ToggleRepository {

    override suspend fun saveToggleState(key: String, isChecked: Boolean) {
        sharedPrefs.edit().putBoolean(key, isChecked).apply()
    }

    override suspend fun getToggleState(key: String): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (changedKey == key) {
                trySend(prefs.getBoolean(key, false))
            }
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)

        trySend(sharedPrefs.getBoolean(key, false))

        awaitClose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}
