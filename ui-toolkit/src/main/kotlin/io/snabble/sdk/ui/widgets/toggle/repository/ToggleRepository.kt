package io.snabble.sdk.ui.widgets.toggle.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal interface ToggleRepository {

    suspend fun saveToggleState(key: String, isChecked: Boolean)

    suspend fun getToggleState(key: String): Flow<Boolean>
}

private const val STORE_NAME = "snabble_toggle_settings"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = STORE_NAME)

internal class ToggleRepositoryImpl(
    private val context: Context,
) : ToggleRepository {

    override suspend fun saveToggleState(key: String, isChecked: Boolean) {
        context.dataStore.edit { mutablePrefs ->
            mutablePrefs[booleanPreferencesKey(key)] = isChecked
        }
    }

    override suspend fun getToggleState(key: String): Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[booleanPreferencesKey(key)] ?: false
        }
}
