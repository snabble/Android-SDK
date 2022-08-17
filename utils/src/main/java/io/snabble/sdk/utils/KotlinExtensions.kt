@file:JvmName("KotlinExtensions")
package io.snabble.sdk.utils

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.core.view.isVisible

fun CharSequence?.isNotNullOrBlank() = !isNullOrBlank()

fun Context.getImageId(resource: String): Int =
    resources.getIdentifier(resource, "drawable", packageName)

fun Context.getResourceId(resource: String): Int =
    resources.getIdentifier(resource,"string", packageName)

fun Context.getResourceString(resource: String): CharSequence =
    resources.getText(getResourceId(resource))

fun TextView.setTextOrHide(text: CharSequence?) {
    this.isVisible = text.isNotNullOrBlank()
    this.text = text
}

fun Context.getColorByAttribute(@AttrRes attrResId: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrResId, typedValue, true)
    return typedValue.data
}

fun Int.toHexString(): String = Integer.toHexString(this)

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