package io.snabble.sdk.screens.shopfinder.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import io.snabble.sdk.extensions.getApplicationInfoCompat
import io.snabble.sdk.utils.BuildConfig
import io.snabble.sdk.widgets.snabble.devsettings.login.usecase.HasEnabledDevSettingsUseCaseImpl

internal class ShopFinderPreferences internal constructor(private val context: Context) {

    val hasGoogleMapsKey: Boolean by lazy {
        context.packageManager.getApplicationInfoCompat(context.packageName)
            .metaData.containsKey(MAPS_API_KEY_NAME)
    }

    val isInDarkMode: Boolean
        get() {
            val currentNightMode =
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return currentNightMode == Configuration.UI_MODE_NIGHT_YES
        }

    private val sharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(context)

    private val debugSharedPreferences: SharedPreferences
        get() = context.getSharedPreferences("DebugManager", Context.MODE_PRIVATE)

    var isHiddenMenuAvailable: Boolean
        get() = debugSharedPreferences.getBoolean(
            KEY_HIDDEN_MENU_AVAILABLE,
            BuildConfig.DEBUG || projectCode != null
        )
        set(available) = debugSharedPreferences.edit()
            .putBoolean(KEY_HIDDEN_MENU_AVAILABLE, available).apply()

    var isDebuggingAvailable: Boolean
        get() = debugSharedPreferences.getBoolean(KEY_DEBUGGING_AVAILABLE, BuildConfig.DEBUG)
        set(available) {
            isHiddenMenuAvailable = available
            debugSharedPreferences.edit().putBoolean(KEY_DEBUGGING_AVAILABLE, available).apply()
        }
    var projectCode: String?
        get() = sharedPreferences.getString(KEY_PROJECT_CODE, null)
        set(projectCode) {
            sharedPreferences
                .edit()
                .putString(KEY_PROJECT_CODE, projectCode)
                .apply()
        }

    var isMapsEnabled: Boolean
        get() = sharedPreferences
            .getBoolean(KEY_MAPS_ENABLED, true)
        set(enabled) {
            sharedPreferences
                .edit()
                .putBoolean(KEY_MAPS_ENABLED, enabled)
                .apply()
        }

    val areDevSettingsEnabled = HasEnabledDevSettingsUseCaseImpl(sharedPreferences)()

    companion object {

        private const val KEY_MAPS_ENABLED = "mapsEnabled"
        private const val KEY_HIDDEN_MENU_AVAILABLE = "hiddenMenuAvailable"
        private const val KEY_DEBUGGING_AVAILABLE = "debuggingAvailable"
        private const val KEY_PROJECT_CODE = "projectCode"

        private const val MAPS_API_KEY_NAME = "com.google.android.geo.API_KEY"

        @SuppressLint("StaticFieldLeak")
        private var instance: ShopFinderPreferences? = null

        fun getInstance(context: Context): ShopFinderPreferences =
            instance ?: ShopFinderPreferences(context.applicationContext)
                .also { instance = it }
    }
}
