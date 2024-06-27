package io.snabble.sdk.ui.cart.shoppingcart.product

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.adapter.widgets.SwipeToDeleteContainer
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem
import io.snabble.sdk.ui.cart.shoppingcart.product.widget.ProductWidget

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
        ProductWidget(
            cartItem = item,
            hasAnyImages = hasAnyImages,
            onDeleteItem = { onItemDeleted(it) }
        )
    }
}
