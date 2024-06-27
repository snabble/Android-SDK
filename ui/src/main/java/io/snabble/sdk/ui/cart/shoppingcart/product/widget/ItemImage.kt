package io.snabble.sdk.ui.cart.shoppingcart.product.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem

@Composable
@OptIn(ExperimentalGlideComposeApi::class)
fun ItemImage(row: ProductItem, hasAnyImages: Boolean) {
    if (row.imageUrl != null) {
        GlideImage(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp)), model = row.imageUrl, contentDescription = row.name
        )
    } else if (hasAnyImages) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
    }
}
