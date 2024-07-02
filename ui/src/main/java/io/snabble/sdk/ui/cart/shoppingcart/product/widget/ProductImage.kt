package io.snabble.sdk.ui.cart.shoppingcart.product.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun ProductImage(
    imageUrl: String?,
    contentDescription: String?,
    showPlaceholder: Boolean,
    isAgeRestricted: Boolean,
    age: Int
) {
    Box(modifier = Modifier.wrapContentSize()) {
        if (imageUrl != null) {
            GlideImage(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)), model = imageUrl, contentDescription = contentDescription
            )
        } else if (showPlaceholder) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
        AgeRestrictionIcon(isAgeRestricted, age)
    }
}
