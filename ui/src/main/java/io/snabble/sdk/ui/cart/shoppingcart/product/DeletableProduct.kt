package io.snabble.sdk.ui.cart.shoppingcart.product

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem
import io.snabble.sdk.ui.cart.shoppingcart.product.widget.Product

@Composable
fun DeletableProduct(
    modifier: Modifier,
    item: ProductItem,
    onItemDeleted: (ShoppingCart.Item) -> Unit,
) {

    SwipeToDeleteContainer(
        item = item,
        modifier = modifier,
        onDelete = { onItemDeleted(it.item) },
    ) {
        Product(
            cartItem = item,
            onDeleteItem = { onItemDeleted(it) }
        )
    }
}
