package io.snabble.sdk.ui.cart.shoppingcart.product.widget.description

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun LineItemDescription(priceText: String?) {
    priceText ?: return
    Text(
        text = priceText,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = Bold,
    )
}
