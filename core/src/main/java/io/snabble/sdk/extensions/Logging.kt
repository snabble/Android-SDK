package io.snabble.sdk.extensions

import android.util.Log

@Deprecated("Only for debugging", replaceWith = ReplaceWith("this"))
fun <T> T.xx(msg: String? = null, tag: String = "xx", throwable: Throwable? = null): T {
    Log.d(tag, "${msg ?: ""} <$this>", throwable)
    return this
}

@Deprecated("Only for debugging", replaceWith = ReplaceWith("this"))
fun logX(msg: String? = null, tag: String = "xx", throwable: Throwable? = null) {
    Log.d(tag, msg ?: "", throwable)
}
