package io.snabble.sdk.ui.cart.shoppingcart.product.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.ThemeWrapper

@Composable
fun ProductWidget(
    cartItem: ProductItem,
    hasAnyImages: Boolean,
    onDeleteItem: (ShoppingCart.Item) -> Unit,
) {

    val hasCoupon = (cartItem.item.coupon != null)
    val isAgeRestricted = cartItem.item.product?.saleRestriction?.isAgeRestriction ?: false
    val age = cartItem.item.product?.saleRestriction?.value ?: 0
    val encodingUnit = cartItem.encodingUnit
    val encodingUnitDisplayName = encodingUnit?.displayValue ?: "g"

    ThemeWrapper {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = 16.dp, horizontal = 16.dp)
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.wrapContentSize()) {
                    ItemImage(cartItem, hasAnyImages)
                    ExtraImage(hasCoupon, isAgeRestricted, age, cartItem.manualDiscountApplied)
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row {

                        PriceDescription(modifier = Modifier.weight(1f), cartItem)
                        if (cartItem.editable && cartItem.item?.product?.type != io.snabble.sdk.Product.Type.UserWeighed) {
                            QuantityField(modifier = Modifier, cartItem, onQuantityChanged = {
                                if (it <= 0) {
                                    cartItem.item?.let { shoppingCartItem ->
                                        onDeleteItem(shoppingCartItem)
                                    }
                                } else {
                                    cartItem.item?.setQuantityMethod(it)
                                    Telemetry.event(Telemetry.Event.CartAmountChanged, cartItem.item?.product)
                                }
                            })
                        }
                        if (cartItem.editable && cartItem.item?.product?.type == io.snabble.sdk.Product.Type.UserWeighed) {
                            UserWeighedField(
                                cartItem.quantity.toString(),
                                encodingUnitDisplayName,
                                onQuantityChanged = {
                                    cartItem.item.setQuantityMethod(it)
                                    Telemetry.event(Telemetry.Event.CartAmountChanged, cartItem.item.product)
                                },
                                onDeleteWeighed = {
                                    cartItem.item?.let { shoppingCartItem ->
                                        onDeleteItem(shoppingCartItem)
                                    }
                                }
                            )
                        }
                    }
                    cartItem.discounts.forEach {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(it.discount, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            Text(it.name, style = MaterialTheme.typography.bodyMedium)
                            Image(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(R.drawable.discount_badge),
                                contentDescription = ""
                            )
                        }
                    }
                }
            }
        }
    }
}
