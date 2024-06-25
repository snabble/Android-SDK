package io.snabble.sdk.ui.cart.shoppingcart.adapter.widgets.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.snabble.sdk.ui.cart.shoppingcart.row.ProductRow

@Composable
fun PriceDescription(modifier: Modifier = Modifier, row: ProductRow) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        row.name?.let {
            Text(
                text = it,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                maxLines = 2,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Row {
            row.priceText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            row.depositPrice?.let {
                Text(
                    text = " + ${row.depositPrice} ${row.depositText}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
