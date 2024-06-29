package io.snabble.sdk.ui.cart.shoppingcart.product.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem

@Composable
fun ProductDescription(modifier: Modifier = Modifier, item: ProductItem) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {

        item.name?.let { productName ->
            Text(
                text = productName,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                maxLines = 2,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.discounts.isNotEmpty()) {
                item.priceText?.let {
                    Text(
                        text = it,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = Bold,
                    )
                }
                item.totalPrice?.let { totalPrice ->
                    Text(
                        text = totalPrice,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.33f
                            ),
                            textDecoration = if (item.discounts.isNotEmpty()) TextDecoration.LineThrough else TextDecoration.None
                        )
                    )
                }
            } else {
                item.totalPrice?.let {
                    Text(
                        text = it,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = Bold,
                    )
                } ?: item.priceText?.let {
                    Text(
                        text = it,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = Bold,
                    )
                }
            }
        }
        item.deposit?.depositPrice?.let {
            Text(
                text = "${item.priceText} + ${item.deposit.depositPriceText} ${item.deposit.depositText}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
