package io.snabble.sdk.ui.cart.shoppingcart

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.CartDiscountWidget
import io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.model.CartDiscountItem
import io.snabble.sdk.ui.cart.shoppingcart.product.DeletableProduct
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem
import io.snabble.sdk.ui.utils.ThemeWrapper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingCartScreen(
    viewModel: ShoppingCartViewModel = ShoppingCartViewModel(),
    onItemDeleted: (item: ShoppingCart.Item, index: Int) -> Unit
) {

    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    ThemeWrapper {
        LazyColumn {
            items(items = uiState.items, key = { it.hashCode() }) { cartItem ->
                when (cartItem) {
                    is ProductItem -> {
                        DeletableProduct(
                            modifier = Modifier.animateItemPlacement(),
                            item = cartItem,
                            onItemDeleted = { shoppingCartItem ->
                                viewModel.onEvent(
                                    RemoveItem(
                                        item = shoppingCartItem,
                                        onSuccess = { index ->
                                            onItemDeleted(shoppingCartItem, index)
                                        }
                                    )
                                )
                            }
                        )
                        HorizontalDivider()
                    }

                    is CartDiscountItem -> {
                        CartDiscountWidget(
                            modifier = Modifier.fillMaxWidth(),
                            item = cartItem
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
