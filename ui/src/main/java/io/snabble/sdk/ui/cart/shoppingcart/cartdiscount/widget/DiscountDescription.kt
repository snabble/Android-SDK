package io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun DiscountDescription(
    title: String,
    description: String,
    discount: String,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = discount,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End
        )
        Icon(
            Icons.Outlined.DeleteOutline,
            modifier = Modifier.clickable(onClick = onDelete),
            contentDescription = null
        )
    }
}
