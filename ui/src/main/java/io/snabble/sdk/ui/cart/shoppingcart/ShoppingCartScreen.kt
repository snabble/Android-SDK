package io.snabble.sdk.ui.cart.shoppingcart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.snabble.sdk.Product
import io.snabble.sdk.checkout.LineItem
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.CartDiscount
import io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.model.CartDiscountItem
import io.snabble.sdk.ui.cart.shoppingcart.depositreturn.DepositReturn
import io.snabble.sdk.ui.cart.shoppingcart.product.DeletableProduct
import io.snabble.sdk.ui.cart.shoppingcart.product.model.DepositItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.DepositReturnItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.DiscountItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem
import io.snabble.sdk.ui.utils.ThemeWrapper

@Composable
fun ShoppingCartScreen(
    modifier: Modifier = Modifier,
    viewModel: ShoppingCartViewModel = viewModel(),
    onItemDeleted: (item: ShoppingCart.Item, index: Int) -> Unit
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    ThemeWrapper {
        ShoppingCartScreen(
            modifier = modifier,
            uiState = uiState,
            onItemDeleted = { item ->
                viewModel.onEvent(
                    RemoveItem(
                        item = item,
                        onSuccess = { index ->
                            onItemDeleted(item, index)
                        }
                    )
                )
            },
            onQuantityChanged = { item, quantity ->
                viewModel.onEvent(UpdateQuantity(item, quantity))
            }
        )
    }
}

@Composable
private fun ShoppingCartScreen(
    uiState: UiState,
    modifier: Modifier = Modifier,
    onItemDeleted: (ShoppingCart.Item) -> Unit,
    onQuantityChanged: (ShoppingCart.Item, Int) -> Unit
) {

    LazyColumn(
        modifier = modifier.nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        itemsIndexed(items = uiState.items, key = { _, item -> item.hashCode() }) { index, cartItem ->
            when (cartItem) {
                is ProductItem -> {
                    DeletableProduct(
                        modifier = Modifier.animateItem(),
                        item = cartItem,
                        onItemDeleted = { onItemDeleted(cartItem.item) },
                        onQuantityChanged = { quantity ->
                            onQuantityChanged(cartItem.item, quantity)
                        }
                    )
                }

                is DepositReturnItem -> {
                    val showHint = uiState.totalCartPrice != null && uiState.totalCartPrice < 0
                    DepositReturn(item = cartItem, showHint = showHint, onDeleteClick = onItemDeleted)
                }

                is CartDiscountItem -> {
                    CartDiscount(
                        modifier = Modifier.fillMaxWidth(),
                        item = cartItem
                    )
                }
            }
            if (index != uiState.items.lastIndex) HorizontalDivider()
        }
    }
}

@Preview
@Composable
private fun CardWithDefaultItems() {
    val uiState = UiState(
        items = listOf(
            ProductItem(
                ShoppingCart.Item(ShoppingCart(), LineItem(name = "Cold brew Coffee")),
                imageUrl = "https://demodata.snabble.io/demodata/images/snabble-cold-brew-coffee.png",
                name = "Cold brew Coffee",
                quantity = 1,
                editable = true,
                quantityText = "1",
                totalPrice = 999,
                totalPriceText = "9,99€",
                unit = "g"
            ),
            ProductItem(
                ShoppingCart.Item(ShoppingCart(), LineItem(name = "Energy Drink")),
                imageUrl = "https://demodata.snabble.io/demodata/images/sdk/energydrink.png",
                name = "Energy Drink",
                quantity = 3,
                editable = true,
                isAgeRestricted = true,
                minimumAge = 18,
                quantityText = "3",
                totalPrice = 100,
                totalPriceText = "3,00€",
                unit = "g"
            )
        )
    )

    ShoppingCartScreen(
        uiState = uiState,
        onItemDeleted = {},
        onQuantityChanged = { _, _ -> }
    )
}

@Preview
@Composable
private fun CardWithDiscountItem() {
    val uiState = UiState(
        items = listOf(
            ProductItem(
                ShoppingCart.Item(ShoppingCart(), LineItem(name = "Cold brew Coffee")),
                imageUrl = "https://demodata.snabble.io/demodata/images/snabble-cold-brew-coffee.png",
                name = "Cold brew Coffee",
                quantity = 1,
                editable = true,
                quantityText = "1",
                discounts = listOf(
                    DiscountItem(
                        name = "5€ Discount",
                        discount = "-5,00€",
                        discountValue = -500
                    )
                ),
                discountedPrice = "4,99€",
                totalPrice = 999,
                totalPriceText = "9,99€",
                unit = "g"
            )
        )
    )


    ShoppingCartScreen(
        uiState = uiState,
        onItemDeleted = {},
        onQuantityChanged = { _, _ -> }
    )
}

@Preview
@Composable
private fun CardWithDeposit() {
    val uiState = UiState(
        items = listOf(
            ProductItem(
                ShoppingCart.Item(ShoppingCart(), LineItem(name = "Cold brew Coffee")),
                imageUrl = "https://demodata.snabble.io/demodata/images/snabble-cold-brew-coffee.png",
                name = "Cold brew Coffee",
                quantity = 1,
                editable = true,
                quantityText = "1",
                deposit = DepositItem(
                    depositPrice = 25,
                    depositPriceText = "0,25€",
                    depositText = "Deposit"
                ),
                discountedPrice = "4,99€",
                totalPrice = 999,
                priceText = "4,99€",
                totalPriceText = "5,24€",
                unit = "g"
            )
        )
    )


    ShoppingCartScreen(
        uiState = uiState,
        onItemDeleted = {},
        onQuantityChanged = { _, _ -> }
    )
}

@Preview
@Composable
private fun CardWithCartDiscount() {
    val uiState = UiState(
        items = listOf(
            ProductItem(
                ShoppingCart.Item(ShoppingCart(), LineItem(name = "Cold brew Coffee")),
                imageUrl = "https://demodata.snabble.io/demodata/images/snabble-cold-brew-coffee.png",
                name = "Cold brew Coffee",
                quantity = 1,
                editable = true,
                quantityText = "1",
                totalPrice = 999,
                totalPriceText = "9,99€",
                unit = "g"
            ),
            CartDiscountItem(
                item = ShoppingCart.Item(ShoppingCart(), LineItem(name = "Multi shopper discount")),
                name = "Multi shopper discount",
                discount = "-5,00€"
            )
        )
    )


    ShoppingCartScreen(
        uiState = uiState,
        onItemDeleted = {},
        onQuantityChanged = { _, _ -> }
    )
}
