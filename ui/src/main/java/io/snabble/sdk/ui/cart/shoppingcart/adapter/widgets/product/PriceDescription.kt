package io.snabble.sdk.ui.cart.shoppingcart.adapter.widgets.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.snabble.sdk.PriceFormatter
import io.snabble.sdk.Snabble
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
        row.depositPriceText?.let {
            Text(
                text = "${row.totalPrice(PriceFormatter(Snabble.projects.first()))}",
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = Bold,
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
                    text = " + ${row.depositPriceText} ${row.depositText}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
