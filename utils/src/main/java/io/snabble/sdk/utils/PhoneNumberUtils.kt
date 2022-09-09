package io.snabble.sdk.utils

import android.annotation.SuppressLint
import android.text.SpannableStringBuilder

/**
 * Collection of utils to format and handle phone numbers
 */
object PhoneNumberUtils {
    /**
     * Convert any phone number to a [CharSequence] with embedded [UrlSpan] to make phone numbers clickable.
     *
     * This method handles formatting which are not supported by [Linkify]. Please note that you still need to set the
     * `movementMethod` on your TextView like this:
     *
     * `textView.movementMethod = LinkMovementMethod.getInstance()`
     */
    fun convertPhoneNumberToUrlSpan(phoneNumber: String): CharSequence {
        val target = convertToTelSchema(phoneNumber)
        val markdown = "[$phoneNumber]($target)"
        return parseLinksInto(markdown, SpannableStringBuilder())
    }

    /**
     * Convert a formatted phone number to a tel schema and returns it.
     */
    fun convertToTelSchema(phoneNumber: String) =
        // remove blocks of '(0)' when not at end, the chars '-', '/', '(' and ')' and also all whitespace
        "tel:" + phoneNumber.replace("(\\(0\\)(?<!\$)|[-/()]|\\s)".toRegex(), "")
}