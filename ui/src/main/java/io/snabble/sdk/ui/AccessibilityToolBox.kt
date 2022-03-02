package io.snabble.sdk.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import io.snabble.sdk.Snabble

fun View.setClickDescription(stringId: Int, vararg formatArgs: Any) {
    setClickDescription(context.getString(stringId, formatArgs))
}

fun View.setClickDescription(description: String) {
    ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
        override fun onInitializeAccessibilityNodeInfo(v: View, info: AccessibilityNodeInfoCompat) {
            super.onInitializeAccessibilityNodeInfo(v, info)
            info.addAction(
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_CLICK,
                    description
                )
            )
        }
    })
}

fun TextView.cleanUpDescription() {
    contentDescription = text.toString().replace("...", "…").replace("snabble", "snäbble")
}

val Context.isTalkBackActive: Boolean
    get() {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val voiceServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).orEmpty()
        val isTouchExplorationEnabled = am?.isTouchExplorationEnabled ?: false
        return voiceServices.isNotEmpty() && isTouchExplorationEnabled
    }

fun View.focusForAccessibility() {
    performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED)
}

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