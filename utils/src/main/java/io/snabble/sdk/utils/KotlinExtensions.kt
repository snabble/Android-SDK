@file:JvmName("KotlinExtensions")
package io.snabble.sdk.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.core.view.isVisible

/**
 * Returns a Boolean. True if the given Charsequence is not null or blank
 */
fun CharSequence?.isNotNullOrBlank() = !isNullOrBlank()

/**
 * Resolves the given into image resource identifier
 */
fun Context.getImageId(resource: String): Int =
    resources.getIdentifier(resource, "drawable", packageName)

/**
 * Resolves the given into string resource identifier
 */
fun Context.getResourceId(resource: String): Int =
    resources.getIdentifier(resource,"string", packageName)

/**
 * Converts the string into a resource id and returns the matching resource String
 */
fun Context.getResourceString(resource: String): CharSequence =
    resources.getText(getResourceId(resource))

/**
 * Sets the textview to visible and text to be displayed if the given char sequence is not null or blank
 */
fun TextView.setTextOrHide(text: CharSequence?) {
    this.isVisible = text.isNotNullOrBlank()
    this.text = text
}

/**
 * Returns the int value of an attribute
 */
fun Context.getColorByAttribute(@AttrRes attrResId: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrResId, typedValue, true)
    return typedValue.data
}

/**
 * Returns a converted hex string matching the previous int value
 */
fun Int.toHexString(): String = Integer.toHexString(this)

/**
 * Sets the textview to visible and the text to be displayed if the given char sequence is not null or blank.
 * The text will be set as resource string if the string matches a string resoruce id. Otherwise the plain text will be displayed
 */
fun TextView.resolveTextOrHide(string: String?) {
    if (string.isNotNullOrBlank()) {
        val resId = context.getResourceId(string!!)
        if (resId != Resources.ID_NULL) {
            setText(resId)
        } else {
            text = string
        }
        isVisible = true
    } else {
        isVisible = false
    }
}