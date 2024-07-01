package io.snabble.sdk.ui.cart.shoppingcart.product.widget.description

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun Discountdescription(totalPrice: String?, discountPrice: String?) {
    if (totalPrice == null || discountPrice == null) return

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = discountPrice,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = Bold,
        )
        Text(
            text = totalPrice,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.33f
                ),
                textDecoration = TextDecoration.LineThrough,
            )
        )
    }
}
