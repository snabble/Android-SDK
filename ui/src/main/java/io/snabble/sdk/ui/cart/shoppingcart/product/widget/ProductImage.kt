package io.snabble.sdk.ui.cart.shoppingcart.product.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.snabble.sdk.ui.cart.shoppingcart.image.RemoteImage

private val ImageSize = 48.dp
private val ImageCornerRadius = 4.dp

private const val HUE_DEGREES = 360
private const val PLACEHOLDER_SATURATION = 0.5f
private const val PLACEHOLDER_BRIGHTNESS = 0.6f

@Composable
internal fun ProductImage(
    imageUrl: String?,
    name: String?,
    contentDescription: String?,
    showPlaceholder: Boolean,
    isAgeRestricted: Boolean,
    age: Int
) {
    val hasAgeBadge = isAgeRestricted && age > 0
    if (imageUrl == null && !showPlaceholder && !hasAgeBadge) return

    Box(modifier = Modifier.wrapContentSize()) {
        val imageModifier = Modifier
            .size(ImageSize)
            .clip(RoundedCornerShape(ImageCornerRadius))

        if (imageUrl != null) {
            val targetSizePx = with(LocalDensity.current) { ImageSize.roundToPx() }
            RemoteImage(
                imageUrl = imageUrl,
                targetSizePx = targetSizePx,
                modifier = imageModifier,
                contentDescription = contentDescription,
                placeholder = { LetterPlaceholder(name) },
                error = { LetterPlaceholder(name) },
            )
        } else if (showPlaceholder) {
            Box(modifier = imageModifier) { LetterPlaceholder(name) }
        }
        AgeRestrictionIcon(isAgeRestricted, age)
    }
}

/**
 * Fallback shown while an image loads or when it is missing: a colored box, tinted deterministically
 * from [name], with the product's leading letter centered on top.
 */
@Composable
private fun BoxScope.LetterPlaceholder(name: String?) {
    val color = remember(name) { placeholderColor(name) }
    val letter = name?.trim()?.firstOrNull()?.uppercaseChar()?.toString().orEmpty()

    Box(
        modifier = Modifier
            .matchParentSize()
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (letter.isNotEmpty()) {
            Text(
                text = letter,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun placeholderColor(name: String?): Color {
    val hue = ((name?.hashCode() ?: 0) % HUE_DEGREES).let { if (it < 0) it + HUE_DEGREES else it }.toFloat()
    return Color.hsv(hue, PLACEHOLDER_SATURATION, PLACEHOLDER_BRIGHTNESS)
}
