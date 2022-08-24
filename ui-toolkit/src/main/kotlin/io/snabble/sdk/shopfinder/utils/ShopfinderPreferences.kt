package io.snabble.sdk.shopfinder.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import io.snabble.sdk.utils.BuildConfig

class ShopfinderPreferences internal constructor(private val context: Context) {
    companion object {

        private const val KEY_MAPS_ENABLED = "mapsEnabled"
        private const val KEY_HIDDEN_MENU_AVAILABLE = "hiddenMenuAvailable"
        private const val KEY_DEBUGGING_AVAILABLE = "debuggingAvailable"
        private const val KEY_PROJECT_CODE = "projectCode"

        @SuppressLint("StaticFieldLeak")
        private var instance: ShopfinderPreferences? = null

        fun getInstance(context: Context): ShopfinderPreferences {
            if (instance == null) {
                instance = ShopfinderPreferences(context)
            }
            return instance!!
        }
    }


    val hasGoogleMapsKey: Boolean by lazy {
        context.applicationContext.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
            .metaData.containsKey("com.google.android.geo.API_KEY")
    }

    val isInDarkMode: Boolean
        get() {
            val currentNightMode =
                context.applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return currentNightMode == Configuration.UI_MODE_NIGHT_YES
        }
    val sharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    val debugSharedPreferences: SharedPreferences
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
}