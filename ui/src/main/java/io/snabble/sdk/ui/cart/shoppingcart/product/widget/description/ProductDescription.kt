package io.snabble.sdk.ui.cart.shoppingcart.product.widget.description

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem

@Composable
internal fun ProductDescription(modifier: Modifier = Modifier, item: ProductItem) {
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        when {
            item.discounts.isNotEmpty() -> Discountdescription(item.totalPriceText, item.discountedPrice)
            else -> LineItemDescription(item.totalPriceText)
        }
        if (item.deposit != null) {
            val depositText = stringResource(id = R.string.Snabble_Shoppingcart_deposit)
            Text(
                text = "${item.priceText} + ${item.deposit.depositPriceText} $depositText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
