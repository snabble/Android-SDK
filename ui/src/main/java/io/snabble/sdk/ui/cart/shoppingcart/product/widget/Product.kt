package io.snabble.sdk.ui.cart.shoppingcart.product.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.snabble.sdk.Product.Type.UserWeighed
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.shoppingcart.product.model.DiscountItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem
import io.snabble.sdk.ui.cart.shoppingcart.product.widget.description.ProductDescription
import io.snabble.sdk.ui.telemetry.Telemetry

@Composable
internal fun Product(
    cartItem: ProductItem,
    onQuantityChanged: (Int) -> Unit,
    onDeleteItem: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 16.dp, horizontal = 16.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProductImage(
            imageUrl = cartItem.imageUrl,
            contentDescription = cartItem.name,
            showPlaceholder = cartItem.showPlaceHolder,
            isAgeRestricted = cartItem.isAgeRestricted,
            age = cartItem.minimumAge
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductDescription(modifier = Modifier.weight(1f), cartItem)
                if (cartItem.editable && cartItem.item.product?.type != UserWeighed) {
                    QuantityField(modifier = Modifier, cartItem, onQuantityChanged = {
                        if (it <= 0) {
                            onDeleteItem()
                        } else {
                            onQuantityChanged(it)
                        }
                    })
                }
                if (cartItem.editable && cartItem.item.product?.type == UserWeighed) {
                    UserWeighedField(
                        cartItem.quantity.toString(),
                        cartItem.unit,
                        onQuantityChanged = {
                            cartItem.item.updateQuantity(it)
                            Telemetry.event(Telemetry.Event.CartAmountChanged, cartItem.item.product)
                        },
                        onDeleteWeighed = onDeleteItem
                    )
                }
            }
            Discounts(cartItem.discounts)
        }
    }
}

@Composable
private fun Discounts(discounts: List<DiscountItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        discounts.forEach {
            DiscountItemWidget(it)
        }
    }
}

@Composable
private fun DiscountItemWidget(it: DiscountItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(it.discount, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Image(
            modifier = Modifier.size(18.dp),
            painter = painterResource(R.drawable.discount_badge),
            contentDescription = ""
        )
    }
}
