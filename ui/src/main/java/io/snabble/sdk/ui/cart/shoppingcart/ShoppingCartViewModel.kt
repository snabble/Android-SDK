package io.snabble.sdk.ui.cart.shoppingcart

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import io.snabble.sdk.PriceFormatter
import io.snabble.sdk.Snabble
import io.snabble.sdk.Unit
import io.snabble.sdk.checkout.LineItemType
import io.snabble.sdk.extensions.xx
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.item.ItemType
import io.snabble.sdk.shoppingcart.data.listener.ShoppingCartListener
import io.snabble.sdk.shoppingcart.data.listener.SimpleShoppingCartListener
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.telemetry.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ShoppingCartViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState(emptyList()))
    val uiState = _uiState.asStateFlow()

    private lateinit var cachedCart: ShoppingCart

    private val shoppingCartListener: ShoppingCartListener = object : SimpleShoppingCartListener() {
        override fun onChanged(list: ShoppingCart?) {
            updateUiState(list)
        }
    }

    init {
        val cart = Snabble.checkedInProject.value?.shoppingCart
        cart?.addListener(shoppingCartListener)
        updateUiState(Snabble.checkedInProject.value?.shoppingCart)
    }

    fun onEvent(event: Event) {
        when (event) {
            is RemoveItem -> removeItemFromCart(event.item, event.onSuccess)
        }
    }

    private fun removeItemFromCart(item: ShoppingCart.Item?, onSuccess: (index: Int) -> kotlin.Unit) {
        val index = cachedCart.indexOf(item)
        if (index != -1) {
            cachedCart.remove(index)
            updateUiState(cachedCart)
            Telemetry.event(Telemetry.Event.DeletedFromCart, item?.product)
            onSuccess(index)
        }
    }

    private fun updateUiState(cart: ShoppingCart?) {
        cart ?: return
        cachedCart = cart

        val cartItems: MutableList<CartItem> = mutableListOf()

        val products = cart.filter { it?.type == ItemType.PRODUCT }.filterNotNull()
        cartItems.addProducts(products)

        val discounts = cart.filter { it?.isDiscount == true && it.lineItem?.discountType != "cart" }.filterNotNull()
        cartItems.addDiscountsToProducts(discounts)

        val deposit = cart.filter { it?.lineItem?.type == LineItemType.DEPOSIT }.filterNotNull()
        cartItems.addDepositsToProducts(deposit)

        val cartDiscount = cart.filter { it?.isDiscount == true && it.lineItem?.discountType == "cart" }.filterNotNull()
        cartItems.addCartDiscount(cartDiscount)

        val giveAway = cart.filter { it?.isGiveaway == true }.filterNotNull()
        cartItems.addGiveAway(giveAway)

        _uiState.update {
            it.copy(
                items = cartItems.xx("new items"),
                hasAnyImages = cart.any { item -> !item?.product?.imageUrl.isNullOrEmpty() }
            )
        }
    }

    private fun MutableList<CartItem>.addProducts(products: List<ShoppingCart.Item>) =
        products.forEach { item: ShoppingCart.Item? ->
            item ?: return@forEach
            add(
                ProductItem(
                    imageUrl = item.product?.imageUrl,
                    name = item.displayName,
                    encodingUnit = item.unit,
                    priceText = item.totalPriceText,
                    quantity = item.getQuantityMethod(),
                    quantityText = item.quantityText,
                    editable = item.isEditable,
                    manualDiscountApplied = item.isManualCouponApplied,
                    item = item,
                )
            )
        }

    private fun MutableList<CartItem>.addDiscountsToProducts(discounts: List<ShoppingCart.Item>) {
        discounts.forEach { item ->
            val name = item.displayName ?: return@forEach
            val discount = item.totalPriceText ?: return@forEach
            val value = item.totalPrice

            firstOrNull { it.item?.id == item.lineItem?.refersTo }
                ?.let {
                    remove(it)
                    val product = it as? ProductItem ?: return@forEach
                    add(
                        product.copy(
                            discounts = it.discounts.plusElement(
                                DiscountItem(
                                    name,
                                    discount,
                                    value
                                )
                            )
                        )
                    )
                }
        }
    }

    private fun MutableList<CartItem>.addDepositsToProducts(deposit: List<ShoppingCart.Item>) =
        deposit.forEach { item ->
            firstOrNull { it.item?.id == item.lineItem?.refersTo }?.let {
                remove(it)
                val product = it as? ProductItem ?: return@forEach
                add(
                    product.copy(
                        deposit = DepositItem(
                            depositPriceText = item.totalPriceText,
                            depositText = item.displayName,
                            depositPrice = item.totalPrice
                        ),
                    )
                )
            }

        }

    private fun MutableList<CartItem>.addGiveAway(giveAway: List<ShoppingCart.Item>) = giveAway.forEach { item ->
        val name = item.displayName ?: return@forEach
        add(
            GiveAwayItem(
                item = item,
                title = name,
            )
        )

    }

    private fun MutableList<CartItem>.addCartDiscount(cartDiscount: List<ShoppingCart.Item>) =
        cartDiscount.forEach { item ->
            val discount = item.priceText ?: return@forEach
            val name = item.displayName ?: return@forEach
            add(
                CartDiscountItem(
                    item = item,
                    discount = discount,
                    name = name
                )
            )
        }
}

sealed interface Event
data class RemoveItem(
    val item: ShoppingCart.Item,
    val onSuccess: (index: Int) -> kotlin.Unit
) : Event

data class UiState(
    val items: List<CartItem>,
    val hasAnyImages: Boolean = false
)


interface CartItem {

    val item: ShoppingCart.Item
    val type: CartItemType
}

sealed interface CartItemType
data object Product : CartItemType
data object CartDiscount : CartItemType
data object GiveAway : CartItemType

data class ProductItem(
    override val item: ShoppingCart.Item,
    override val type: CartItemType = Product,
    val discounts: List<DiscountItem> = mutableListOf(),
    val deposit: DepositItem? = null,
    val name: String? = null,
    val imageUrl: String? = null,
    val encodingUnit: Unit? = null,
    val priceText: String? = null,
    val quantityText: String? = null,
    val quantity: Int = 0,
    val editable: Boolean = false,
    val manualDiscountApplied: Boolean = false,
) : CartItem {

    fun totalPrice(formatter: PriceFormatter): String? {
        val discountPrice = discounts.sumOf { it.discountValue }
        item?.totalPrice ?: return null
        return formatter.format(item.totalPrice + (deposit?.depositPrice ?: 0) + discountPrice)
    }
}

data class CartDiscountItem(
    override val item: ShoppingCart.Item,
    override val type: CartItemType = CartDiscount,
    @StringRes val title: Int = R.string.Snabble_Shoppingcart_discounts,
    val discount: String,
    val name: String,
    @DrawableRes val imageResId: Int = R.drawable.snabble_ic_percent
) : CartItem

data class GiveAwayItem(
    override val item: ShoppingCart.Item,
    override val type: CartItemType = GiveAway,
    val title: String,
    @DrawableRes val imageResId: Int = R.drawable.snabble_ic_gift,
    @StringRes val name: Int = R.string.Snabble_Shoppingcart_giveaway
) : CartItem

data class DiscountItem(
    val name: String,
    val discount: String,
    val discountValue: Int
)

data class DepositItem(
    val depositPrice: Int? = null,
    val depositPriceText: String? = null,
    val depositText: String? = null,
)
