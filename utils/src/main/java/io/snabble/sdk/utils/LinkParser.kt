package io.snabble.sdk.utils

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.widget.TextView
import kotlin.math.min

// Scan the html for a tags and make them clickable with an URLSpan
fun TextView.setClickableLinks(input: CharSequence) {
    text = parseLinksInto(input, SpannableStringBuilder())
    if (movementMethod !is LinkMovementMethod) {
        movementMethod = LinkMovementMethod.getInstance()
    }
}

private fun String.indexOfOrEnd(string: String, start: Int = 0): Int {
    val index = indexOf(string, start)
    return if (index == -1) lastIndex else index
}

private val CharSequence.hasValidHtmlLink
    get() = hasCorrectOrder("<a", "href", ">", "</a>", ignoreCase = true)

private val CharSequence.hasValidMarkdownLink
    get() = hasCorrectOrder("[", "](", ")")

private fun CharSequence.hasCorrectOrder(vararg snipit: String, ignoreCase: Boolean = false): Boolean {
    if (snipit.size < 2) return true
    var last = 0
    val positions = snipit.map { str -> indexOf(str, last.coerceAtMost(lastIndex), ignoreCase).also { last = it + 1 } }
    last = positions.first()
    return positions.drop(1).all { next ->
        (last < next).also {
            last = next
        }
    }
}

internal fun parseLinksInto(input: CharSequence, spannable: SpannableStringBuilder): CharSequence {
    val hasHtml = input.hasValidHtmlLink
    val hasMarkdown = input.hasValidMarkdownLink
    if (hasMarkdown) {
        extractMarkdownLinks(input, spannable)
    } else if (hasHtml) {
        extractHtmlLinks(input, spannable)
    } else {
        spannable.append(input)
    }
    return spannable
}

private fun extractHtmlLinks(input: CharSequence, spannable: SpannableStringBuilder) {
    var pos = 0
    do {
        val tagStart = input.indexOf("<a", pos, ignoreCase = true)
        if (tagStart == -1) {
            spannable.append(input.substring(pos, input.length))
            pos = input.length
        } else {
            spannable.append(input.substring(pos, tagStart))
            val tagEnd = input.toString().indexOfOrEnd(">", tagStart) + 1
            if (tagEnd > tagStart) {
                val tag = input.substring(tagStart, tagEnd)
                val attrStart = tag.indexOf("href", ignoreCase = true)
                if (attrStart > 0) {
                    val delim = tag[min(tag.indexOfOrEnd("'"), tag.indexOfOrEnd("\""))].toString()
                    val valueStart = tag.indexOfOrEnd(delim) + 1
                    val valueEnd = tag.indexOfOrEnd(delim, valueStart)
                    val target = tag.substring(valueStart, valueEnd)
                    val close = input.indexOf("</a>", pos, ignoreCase = true)
                    if (close == -1) throw IllegalStateException("</a> expected")
                    val start = spannable.length
                    spannable.append(input.substring(tagEnd, close))
                    spannable.setSpan(
                        URLSpan(target),
                        start,
                        spannable.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    pos = close + 4
                }
            }
        }
    } while (pos < input.length)
}

private fun extractMarkdownLinks(input: CharSequence, spannable: SpannableStringBuilder) {
    var pos = 0
    do {
        val linkDivider = input.indexOf("](", pos)
        if (linkDivider == -1) {
            spannable.append(input.substring(pos, input.length))
            pos = input.length
        } else {
            val linkStart = input.substring(0, linkDivider).lastIndexOf("[")
            spannable.append(input.substring(pos, linkStart))
            val linkEnd = input.toString().indexOfOrEnd(")", linkStart) + 1
            if (linkEnd > linkDivider && linkDivider + 2 < linkEnd) {
                val label = input.substring(linkStart + 1, linkDivider)
                val target = input.substring(linkDivider + 2, linkEnd - 1)
                val start = spannable.length
                spannable.append(label)
                spannable.setSpan(
                    URLSpan(target),
                    start,
                    spannable.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                pos = linkEnd
            } else {
                spannable.append(input.substring(pos, linkEnd))
                pos = linkEnd
            }
        }
    } while (pos < input.length)
}