package io.snabble.sdk.ui.utils

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import io.snabble.sdk.utils.StringNormalizer

inline var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

fun TextView.setOrHide(text: CharSequence?) {
    this.isVisible = text.isNotNullOrBlank()
    this.text = text
}

fun CharSequence?.isNotNullOrBlank() = !isNullOrBlank()

fun String.highlight(query: String): SpannableString {
    val normalizedText = StringNormalizer.normalize(this)
    val sb = SpannableString(this)
    query.split(" ")
            .filter { it.isNotEmpty() }
            .forEach { q ->
                var lastIndex = 0
                while (true) {
                    lastIndex = normalizedText.indexOf(q, lastIndex)
                    if (lastIndex == -1) {
                        break
                    }
                    val styleSpan = StyleSpan(Typeface.BOLD)
                    sb.setSpan(styleSpan, lastIndex, lastIndex + q.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                    lastIndex += q.length
                }
            }
    return sb
}