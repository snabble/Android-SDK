@file:JvmName("AccessibilityToolBox")
package io.snabble.sdk.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isVisible
import io.snabble.sdk.Snabble

class AccessibilityToolBox2(private val target: View): AccessibilityDelegateCompat() {
    private val eventListeners = mutableMapOf<Int, Pair<Boolean, (event: AccessibilityEvent) -> Any>>()
    private var clickAction: String? = null
    private var longClickAction: String? = null
    val isTalkBackActive
        get() = target.context.isTalkBackActive
    var phoneticDict: Map<String, String> = emptyMap()
        set(value) {
            field = value
            (target.contentDescription?.toString() ?: (target as? TextView)?.text?.toString())?.let { currentValue ->
                var newValue = currentValue
                field.entries.forEach { (from, to) ->
                    newValue = newValue.replace(from, to)
                }
                if (newValue != currentValue) {
                    target.contentDescription = newValue
                }
            }
        }
    private var onInitializeAccessibilityNodeInfo: ((info: AccessibilityNodeInfoCompat)-> Unit)? = null
    fun onInitializeAccessibilityNodeInfo(block: (info: AccessibilityNodeInfoCompat)-> Unit) {
        onInitializeAccessibilityNodeInfo = block
    }

    override fun onPopulateAccessibilityEvent(host: View?, event: AccessibilityEvent?) {
        val (callSuper, listener) = event?.let { eventListeners[event.eventType] } ?: true to null
        if (callSuper) super.onPopulateAccessibilityEvent(host, event)
        listener?.invoke(event!!)
    }

    override fun onInitializeAccessibilityNodeInfo(
        host: View,
        info: AccessibilityNodeInfoCompat
    ) {
        super.onInitializeAccessibilityNodeInfo(host, info)
        clickAction?.let {
            info.addAction(
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_CLICK,
                    clickAction
                )
            )
        }
        longClickAction?.let {
            info.addAction(
                AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_LONG_CLICK,
                    longClickAction
                )
            )
        }
        onInitializeAccessibilityNodeInfo?.invoke(info)
    }

    fun onAccessibilityEvent(@EventType event: Int, block: (event: AccessibilityEvent) -> Any) {
        eventListeners[event] = true to block
    }

    fun replaceAccessibilityEvent(@EventType event: Int, block: (event: AccessibilityEvent) -> Any) {
        eventListeners[event] = false to block
    }

    fun setLongClickAction(action: String, onLongClick: (() -> Any)? = null) {
        longClickAction = action
        onLongClick?.let {
            target.setOnLongClickListener {
                onLongClick()
                true
            }
        }
    }

    fun setLongClickAction(@StringRes action: Int, onLongClick: (() -> Any)? = null) =
        setLongClickAction(target.context.getString(action), onLongClick)

    fun setClickAction(action: String, onClick: (() -> Any)? = null) {
        clickAction = action
        onClick?.let {
            target.setOnClickListener {
                onClick()
            }
        }
    }

    fun setClickAction(@StringRes action: Int, onLongClick: (() -> Any)? = null) =
        setClickAction(target.context.getString(action), onLongClick)
}

@IntDef(
    flag = true,
    value = [AccessibilityEvent.TYPE_VIEW_CLICKED, AccessibilityEvent.TYPE_VIEW_LONG_CLICKED, AccessibilityEvent.TYPE_VIEW_SELECTED, AccessibilityEvent.TYPE_VIEW_FOCUSED, AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT, AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START, AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END, AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, AccessibilityEvent.TYPE_VIEW_SCROLLED, AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED, AccessibilityEvent.TYPE_ANNOUNCEMENT, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED, AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY, AccessibilityEvent.TYPE_GESTURE_DETECTION_START, AccessibilityEvent.TYPE_GESTURE_DETECTION_END, AccessibilityEvent.TYPE_TOUCH_INTERACTION_START, AccessibilityEvent.TYPE_TOUCH_INTERACTION_END, AccessibilityEvent.TYPE_WINDOWS_CHANGED, AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED, AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT]
)
@Retention(AnnotationRetention.SOURCE)
annotation class EventType

fun View.accessibility(block: AccessibilityToolBox2.() -> Any) {
    val toolbox = AccessibilityToolBox2(this)
    if (toolbox.isTalkBackActive) {
        block(toolbox)
        ViewCompat.setAccessibilityDelegate(this, toolbox)
    }
}

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

fun orderViewsForAccessibility(vararg views: View?) {
    views
        .filterNotNull()
        .filter { it.isVisible }
        .forEachWindow { before, current, after ->
        ViewCompat.setAccessibilityDelegate(current, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.setTraversalBefore(after)
                info.setTraversalAfter(before)
            }
        })
    }
}

/**
 * Iterate over an Iterable the iterator param last can be only `null` when the size is 1. The last
 * parameter can only be `null` when the size is less then 2, otherwise both params are not `null`.
 */
fun <T> Iterable<T>.forEachWindow(iterator: (last: T?, current: T, next: T?) -> Unit) {
    val list = toList()
    when {
        list.size == 1 ->
            iterator(null, first(), null)
        list.size == 2 ->
            iterator(first(), last(), null)
        list.size >= 2 -> {
            for (i in 1..count() - 2) {
                iterator(list[i - 1], list[i], list[i + 1])
            }
        }
    }
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