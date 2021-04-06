package io.snabble.sdk.ui.utils

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import io.snabble.sdk.utils.StringNormalizer

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