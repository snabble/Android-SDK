package io.snabble.sdk.ui.cart.shoppingcart.adapter.widgets.simple

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ItemDesciption(
    modifier: Modifier = Modifier,
    title: String?,
    price: String?
) {
    Column(modifier = modifier) {
        title?.let {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        price?.let {
            Text(text = price, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
