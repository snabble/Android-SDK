package io.snabble.sdk.utils

import android.util.Log
import java.util.regex.Pattern

private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
private const val CALL_STACK_INDEX = 2
private var isEnabled: Boolean = BuildConfig.DEBUG

private fun getTag(): String? {
    val stackTrace = Throwable().stackTrace
    if (stackTrace.size <= CALL_STACK_INDEX) {
        return "Logger"
    }
    val stackTraceElement = stackTrace[CALL_STACK_INDEX]
    var className = stackTraceElement.className
    val m = ANONYMOUS_CLASS.matcher(className)
    if (m.find()) {
        className = m.replaceAll("")
    }
    val tag = className.substring(className.lastIndexOf('.') + 1)
    val lineNumber = stackTraceElement.lineNumber
    return "($tag.kt:$lineNumber)"
}

fun log(message: String?, vararg args: Any?) {
    if (message != null) {
        try {
            Log.i(getTag(), String.format(message, *args))
        } catch (e: Exception) {
            // ignore any possible errors while formatting the string
        }
    }
}

fun logDebug(message: String?, vararg args: Any?) {
    if (isEnabled && message != null) {
        try {
            Log.d(getTag(), String.format(message, *args))
        } catch (e: Exception) {
            // ignore any possible errors while formatting the string
        }
    }
}

fun logError(message: String?, vararg args: Any?) {
    if (isEnabled && message != null) {
        try {
            Log.e(getTag(), String.format(message, *args))
        } catch (e: Exception) {
            // ignore any possible errors while formatting the string
        }
    }
}

@Deprecated("Only for debugging", replaceWith = ReplaceWith("this"))
fun <T> T.xx(msg: String? = null, tag: String = "xx", throwable: Throwable? = null): T {
    Log.d(tag, "${msg ?: ""} <$this>", throwable)
    return this
}
