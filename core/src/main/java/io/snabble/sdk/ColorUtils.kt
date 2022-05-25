package io.snabble.sdk

import android.graphics.Color
import androidx.annotation.ColorInt

object ColorUtils {
    @JvmStatic
    fun parseColor(color: String?, @ColorInt default: Int) =
        color?.let {
            Color.parseColor(when {
                "^[0-9a-fA-F]{6}(?:[0-9a-fA-F]{2})?$".toRegex().matches(color) -> {
                    // add missing prefix
                    "#$color"
                }
                "^#?[0-9a-fA-F]{3}$".toRegex().matches(color) -> {
                    // convert 3 digit color to 6 digits
                    color.removePrefix("#").toCharArray()
                        .joinToString(separator = "", prefix = "#") { "$it$it" }
                }
                else -> {
                    color
                }
            })
        } ?: default
}