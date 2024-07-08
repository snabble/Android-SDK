package io.snabble.sdk.ui.cart.shoppingcart.product

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem
import io.snabble.sdk.ui.cart.shoppingcart.product.widget.Product

@Composable
internal fun DeletableProduct(
    modifier: Modifier,
    item: ProductItem,
    onItemDeleted: () -> Unit,
    onQuantityChanged: (Int) -> Unit
) {

    SwipeToDeleteContainer(
        modifier = modifier,
        onDelete = onItemDeleted,
    ) {
        Product(
            cartItem = item,
            onDeleteItem = onItemDeleted,
            onQuantityChanged = onQuantityChanged
        )
    }
}
