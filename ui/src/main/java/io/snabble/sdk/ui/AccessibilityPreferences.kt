package io.snabble.sdk.ui

import android.content.Context
import io.snabble.sdk.Snabble

object AccessibilityPreferences {
    private const val KEY_SUPPRESS_SCANNER_HINT = "suppress_scanner_hint"
    private val sharedPreferences = Snabble.application.getSharedPreferences("accessibility", Context.MODE_PRIVATE)
    var suppressScannerHint: Boolean
        get() = sharedPreferences.getBoolean(KEY_SUPPRESS_SCANNER_HINT, false)
        set(seen) {
            sharedPreferences
                .edit()
                .putBoolean(KEY_SUPPRESS_SCANNER_HINT, seen)
                .apply()
        }
}