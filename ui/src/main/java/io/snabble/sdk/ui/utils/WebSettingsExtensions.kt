package io.snabble.sdk.ui.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.webkit.WebSettings
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

fun WebSettings.enableDarkModeCompat(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            val allow = true
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(this, allow)
        }
    } else {
        val isAppInDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        if (isAppInDarkMode && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            @Suppress("DEPRECATION")
            WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_ON)
        }
    }
}
