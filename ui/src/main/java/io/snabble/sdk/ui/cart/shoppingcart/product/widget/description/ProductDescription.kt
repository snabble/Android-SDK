package io.snabble.sdk.ui.cart.shoppingcart.product.widget.description

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
                style = MaterialTheme.typography.bodyLarge
            )
        }
        when {
            item.discounts.isNotEmpty() -> Discountdescription(item.totalPrice, item.discountPrice)
            else -> LineItemDescription(item.totalPrice)
        }
        if (item.deposit != null) {
            Text(
                text = "${item.priceText} + ${item.deposit.depositPriceText} ${item.deposit.depositText}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
