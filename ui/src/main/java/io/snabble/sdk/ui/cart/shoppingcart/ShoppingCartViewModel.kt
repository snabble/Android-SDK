package io.snabble.sdk.ui.cart.shoppingcart

import androidx.lifecycle.ViewModel
import io.snabble.sdk.PriceFormatter
import io.snabble.sdk.Snabble
import io.snabble.sdk.checkout.LineItemType
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.item.ItemType
import io.snabble.sdk.shoppingcart.data.listener.SimpleShoppingCartListener
import io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.model.CartDiscountItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.DepositItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.DiscountItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem
import io.snabble.sdk.ui.telemetry.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ShoppingCartViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState(emptyList()))
    val uiState = _uiState.asStateFlow()

    private lateinit var cachedCart: ShoppingCart
    private lateinit var priceFormatter: PriceFormatter

    private val simpleShoppingCartListener = object : SimpleShoppingCartListener() {
        override fun onChanged(cart: ShoppingCart) {
            updateUiState(cart)
        }
    }

    init {
        val project = Snabble.checkedInProject.value
        val cart = Snabble.checkedInProject.value?.shoppingCart
        cart?.addListener(simpleShoppingCartListener)
        project?.let {
            priceFormatter = PriceFormatter(it)
            updateUiState(it.shoppingCart)
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is RemoveItem -> removeItemFromCart(event.item, event.onSuccess)
            is UpdateQuantity -> updateQuantity(event.item, event.quantity)
        }
    }

    private fun removeItemFromCart(item: ShoppingCart.Item?, onSuccess: (index: Int) -> Unit) {
        val index = cachedCart.indexOf(item)
        if (index != -1) {
            cachedCart.remove(index)
            Telemetry.event(Telemetry.Event.DeletedFromCart, item?.product)
            onSuccess(index)
        }
    }

    private fun updateQuantity(item: ShoppingCart.Item, quantity: Int) {
        item.updateQuantity(quantity)
        Telemetry.event(Telemetry.Event.CartAmountChanged, item.product)
    }

    private fun updateUiState(cart: ShoppingCart) {
        cachedCart = cart

        val cartItems: MutableList<CartItem> = mutableListOf()

        val products = cart.filter { it?.type == ItemType.PRODUCT }.filterNotNull()
        cartItems.addProducts(products)

        val discounts = cart.filter { it?.isDiscount == true && it.lineItem?.discountType != "cart" }.filterNotNull()
        cartItems.addDiscountsToProducts(discounts)

        cartItems.addPriceModifiersAsDiscountsProducts()

        val deposit = cart.filter { it?.lineItem?.type == LineItemType.DEPOSIT }.filterNotNull()
        cartItems.addDepositsToProducts(deposit)

        val cartDiscount = cart.filter { it?.isDiscount == true && it.lineItem?.discountType == "cart" }.filterNotNull()
        cartItems.addCartDiscount(cartDiscount)

        cartItems.updatePrices()
        cartItems.sortCartDiscountsToBottom()

        _uiState.update { it.copy(items = cartItems) }
    }

    private fun MutableList<CartItem>.sortCartDiscountsToBottom() {
        sortBy { it.item.displayName }
        sortWith(
            compareBy {
                when (it) {
                    is ProductItem -> -1
                    else -> 0
                }
            }
        )
    }

    private fun MutableList<CartItem>.updatePrices() = replaceAll { item ->
        when {
            item is ProductItem -> {
                // Since the total price can be null as we invalidate the online prices,'
                // we need to use the price text instead to display the product price without an changed instead
                val price = item.calculateTotalPrice()
                val priceText = if (price == 0) item.priceText else priceFormatter.format(price)

                item.copy(
                    totalPriceText = priceText,
                    discountedPrice = when {
                        item.discounts.isNotEmpty() -> priceFormatter.format(item.getPriceWithDiscountsApplied())
                        else -> null
                    }
                )
            }

            else -> item
        }
    }

    private fun MutableList<CartItem>.addProducts(products: List<ShoppingCart.Item>) {
        val hasAnyProductAnImage = products.any { !it.product?.imageUrl.isNullOrEmpty() }
        addAll(
            products.map { item ->
                ProductItem(
                    imageUrl = item.product?.imageUrl,
                    showPlaceHolder = hasAnyProductAnImage,
                    name = item.displayName,
                    unit = item.unit?.displayValue ?: "g",
                    priceText = item.totalPriceText,
                    quantity = item.getUnitBasedQuantity(),
                    quantityText = item.quantityText,
                    editable = item.isEditable,
                    isManualDiscountApplied = item.isManualCouponApplied,
                    isAgeRestricted = item.isAgeRestricted,
                    minimumAge = item.minimumAge,
                    item = item,
                    totalPrice = item.lineItem?.totalPrice ?: 0
                )
            }
        )
    }

    private fun MutableList<CartItem>.addPriceModifiersAsDiscountsProducts() = replaceAll { item ->
        when {
            item is ProductItem && item.item.lineItem?.priceModifiers?.isNotEmpty() == true -> {
                val discounts = mutableListOf<DiscountItem>()
                item.item.lineItem?.priceModifiers?.forEach { modifier ->
                    val name = modifier.name ?: return@forEach
                    val modifiedPrice = modifier.convertPriceModifier(
                        item.quantity,
                        item.item.lineItem?.weightUnit,
                        item.item.lineItem?.referenceUnit
                    ).intValueExact()
                    discounts.add(
                        DiscountItem(
                            name = name,
                            discount = priceFormatter.format(
                                modifiedPrice
                            ),
                            discountValue = 0
                        )
                    )
                }
                item.copy(discounts = item.discounts.plus(discounts))
            }

            else -> item
        }
    }

    private fun MutableList<CartItem>.addDiscountsToProducts(discounts: List<ShoppingCart.Item>) {
        discounts.forEach { item ->
            val name = item.displayName ?: return@forEach
            val discount = item.totalPriceText ?: return@forEach
            val value = item.totalPrice

            firstOrNull { it.item.id == item.lineItem?.refersTo }
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
            firstOrNull { it.item.id == item.lineItem?.refersTo }?.let {
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
internal data class RemoveItem(
    val item: ShoppingCart.Item,
    val onSuccess: (index: Int) -> Unit
) : Event

internal data class UpdateQuantity(
    val item: ShoppingCart.Item,
    val quantity: Int
) : Event

data class UiState(
    val items: List<CartItem>,
)
