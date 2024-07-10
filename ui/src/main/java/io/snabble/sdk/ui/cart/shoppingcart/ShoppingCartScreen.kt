package io.snabble.sdk.ui.cart.shoppingcart

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.CartDiscount
import io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.model.CartDiscountItem
import io.snabble.sdk.ui.cart.shoppingcart.product.DeletableProduct
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem
import io.snabble.sdk.ui.utils.ThemeWrapper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingCartScreen(
    viewModel: ShoppingCartViewModel = viewModel(),
    onItemDeleted: (item: ShoppingCart.Item, index: Int) -> Unit
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    ThemeWrapper {
        LazyColumn(
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())
        ) {
            items(items = uiState.items, key = { it.hashCode() }) { cartItem ->
                when (cartItem) {
                    is ProductItem -> {
                        DeletableProduct(
                            modifier = Modifier.animateItemPlacement(),
                            item = cartItem,
                            onItemDeleted = {
                                viewModel.onEvent(
                                    RemoveItem(
                                        item = cartItem.item,
                                        onSuccess = { index ->
                                            onItemDeleted(cartItem.item, index)
                                        }
                                    )
                                )
                            },
                            onQuantityChanged = { quantity ->
                                viewModel.onEvent(UpdateQuantity(cartItem.item, quantity))
                            }
                        )
                        HorizontalDivider()
                    }

                    is CartDiscountItem -> {
                        CartDiscount(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background),
                            item = cartItem
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
