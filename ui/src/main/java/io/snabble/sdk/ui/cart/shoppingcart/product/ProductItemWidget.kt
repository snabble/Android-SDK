package io.snabble.sdk.ui.cart.shoppingcart.product

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.Product
import io.snabble.sdk.ui.cart.shoppingcart.ProductItem
import io.snabble.sdk.ui.cart.shoppingcart.adapter.widgets.SwipeToDeleteContainer

@Composable
fun ProductItemWidget(
    modifier: Modifier,
    item: ProductItem,
    onItemDeleted: (ShoppingCart.Item) -> Unit,
    hasAnyImages: Boolean
) {

    SwipeToDeleteContainer(
        item = item,
        modifier = modifier,
        onDelete = { onItemDeleted(it.item) },
    ) {
        Product(
            cartItem = item,
            hasAnyImages = hasAnyImages,
            onDeleteItem = { onItemDeleted(it) }
        )
    }
}
