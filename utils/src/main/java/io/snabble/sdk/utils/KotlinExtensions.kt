@file:JvmName("KotlinExtensions")

package io.snabble.sdk.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Returns a Boolean. True if the given Charsequence is not null or blank
 */
@OptIn(ExperimentalContracts::class)
fun CharSequence?.isNotNullOrBlank(): Boolean {
    contract { returns(true) implies (this@isNotNullOrBlank != null) }
    return !isNullOrBlank()
}

/**
 * Resolves the given into image string into resource identifier
 * returns the id if found else 0
 */
fun Context.getImageId(resource: String): Int =
    resources.getIdentifier(resource, "drawable", packageName)

/**
 * Resolves the given into image string into resource identifier
 * returns the id if found else null
 */
fun Context.resolveImageId(resource: String?): Int? {
    resource ?: return null
    val resId = getImageId(resource)
    return if (resId != Resources.ID_NULL) resId else null
}

/**
 * Resolves the given into color string into resource identifier
 * returns the id if found else 0
 */
fun Context.getColorId(resource: String): Int =
    resources.getIdentifier(resource, "color", packageName)

/**
 * Resolves the given color string into resource identifier
 * returns the id on success else null
 */
fun Context.resolveColorId(resource: String?): Int? {
    resource ?: return null
    val resId = getColorId(resource)
    return if (resId != Resources.ID_NULL) resId else null
}

/**
 * Resolves the given color string into a color for Compose
 * returns the color as Int on success else null
 */
fun Context.getComposeColor(resource: String?): Int? {
    resource ?: return null
    val resId = resolveColorId(resource)
    resId ?: return null

    val color = ContextCompat.getColor(this, resId).toHexStringWithPrefix()
    return Color.parseColor(color)
}

/**
 * Resolves the given into string resource identifier
 */
fun Context.getResourceId(resource: String): Int =
    resources.getIdentifier(resource, "string", packageName)

fun Context.resolveResourceString(resource: String): CharSequence {
    return if (getResourceId(resource) == Resources.ID_NULL) {
        resource
    } else {
        getResourceString(resource)
    }
}

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

fun Int.toHexStringWithPrefix(): String = "#" + Integer.toHexString(this)

/**
 * Sets the textview to visible and the text to be displayed if the given char sequence is not null or blank.
 * The text will be set as resource string if the string matches a string resoruce id. Otherwise the plain text will be displayed
 */
fun TextView.resolveTextOrHide(string: String?) {
    isVisible = if (string.isNotNullOrBlank()) {
        val resId = context.getResourceId(string)
        if (resId != Resources.ID_NULL) {
            setClickableLinks(context.getText(resId))
        } else {
            setClickableLinks(string)
        }
        true
    } else {
        false
    }
}
