package io.snabble.sdk.ui.cart.shoppingcart.cartdiscount

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.snabble.sdk.checkout.LineItem
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.model.CartDiscountItem
import io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.widget.DiscountDescription

@Composable
internal fun CartDiscount(
    modifier: Modifier,
    item: CartDiscountItem,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            modifier = Modifier.size(44.dp),
            painter = painterResource(id = item.imageResId),
            contentDescription = stringResource(id = item.title),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )
        DiscountDescription(
            title = stringResource(item.title),
            description = item.name,
            discount = item.discount
        )
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    Surface {
        CartDiscount(
            modifier = Modifier.fillMaxWidth(),
            item = CartDiscountItem(
                item = ShoppingCart.Item(ShoppingCart(), LineItem(id = "", amount = 1)),
                discount = "7.00",
                name = "SUPER DUPER RABATT"
            )
        )
    }
}
