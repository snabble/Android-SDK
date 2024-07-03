package io.snabble.sdk.ui.cart.shoppingcart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.PriceFormatter
import io.snabble.sdk.Snabble
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.checkout.LineItemType
import io.snabble.sdk.extensions.xx
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.Taxation
import io.snabble.sdk.shoppingcart.data.item.ItemType
import io.snabble.sdk.shoppingcart.data.listener.ShoppingCartListener
import io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.model.CartDiscountItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.DepositItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.DiscountItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem
import io.snabble.sdk.ui.telemetry.Telemetry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShoppingCartViewModel : ViewModel(), ShoppingCartListener {

    private val _uiState = MutableStateFlow(UiState(emptyList()))
    val uiState = _uiState.asStateFlow()

    private lateinit var cachedCart: ShoppingCart
    private lateinit var priceFormatter: PriceFormatter

    private var updateJob: Job? = null

    init {
        val project = Snabble.checkedInProject.value
        val cart = Snabble.checkedInProject.value?.shoppingCart
        cart?.addListener(this)
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

    private fun update(cart: ShoppingCart) {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            delay(300)
            updateUiState(cart)
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
        cartItems.sort()

        cartItems.xx("items")

        _uiState.update {
            it.copy(
                items = cartItems,
            )
        }
    }

    private fun MutableList<CartItem>.sort() {
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

    private fun MutableList<CartItem>.updatePrices() {
        val modifiedItem = mutableListOf<CartItem>()
        with(iterator()) {
            forEach {
                if (it is ProductItem) {
                    remove()
                    val price = it.getTotalPrice()
                    val priceText = if (price == 0) {
                        it.priceText
                    } else {
                        priceFormatter.format(it.getTotalPrice())
                    }

                    modifiedItem.add(
                        it.copy(
                            totalPrice = priceText,
                            discountPrice = if (it.discounts.isNotEmpty()) priceFormatter.format(it.getDiscountPrice()) else null
                        )
                    )
                }
            }
        }
        addAll(modifiedItem)
    }

    private fun MutableList<CartItem>.addProducts(products: List<ShoppingCart.Item>) {
        val hasAnyProductAnImage = products.any { !it.product?.imageUrl.isNullOrEmpty() }
        products.forEach { item: ShoppingCart.Item? ->
            item ?: return@forEach
            add(
                ProductItem(
                    imageUrl = item.product?.imageUrl,
                    showPlaceHolder = hasAnyProductAnImage,
                    name = item.displayName,
                    unit = item.unit?.displayValue ?: "g",
                    priceText = item.totalPriceText,
                    quantity = item.getQuantityMethod(),
                    quantityText = item.quantityText,
                    editable = item.isEditable,
                    manualDiscountApplied = item.isManualCouponApplied,
                    isAgeRestricted = item.isAgeRestricted,
                    minimumAge = item.minimumAge,
                    item = item,
                    listPrice = item.lineItem?.listPrice ?: 0
                )
            )
        }
    }

    private fun MutableList<CartItem>.addPriceModifiersAsDiscountsProducts() {
        val modifiedItem = mutableListOf<CartItem>()
        with(iterator()) {
            forEach { item ->
                if (item is ProductItem) {
                    remove()
                    val discounts = mutableListOf<DiscountItem>()
                    item.item.lineItem?.priceModifiers?.forEach {
                        discounts.add(
                            DiscountItem(
                                name = it.name ?: "",
                                discount = priceFormatter.format(it.price * item.quantity),
                                discountValue = it.price * item.quantity
                            )
                        )
                    }
                    modifiedItem.add(
                        item.copy(discounts = item.discounts.plus(discounts))
                    )
                }
            }
        }
        addAll(modifiedItem)
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

    override fun onItemAdded(cart: ShoppingCart, item: ShoppingCart.Item) {}

    override fun onQuantityChanged(cart: ShoppingCart, item: ShoppingCart.Item) {
        "updateQuan ${System.currentTimeMillis()}".xx()
        update(cart)
    }

    override fun onCleared(cart: ShoppingCart) {
        TODO("Not yet implemented")
    }

    override fun onItemRemoved(cart: ShoppingCart, item: ShoppingCart.Item, pos: Int) {
    }

    override fun onProductsUpdated(cart: ShoppingCart) {
    }

    override fun onPricesUpdated(cart: ShoppingCart) {
        "updateAfterPrices ${System.currentTimeMillis()}".xx()
        update(cart)
    }

    override fun onCheckoutLimitReached(cart: ShoppingCart) {
    }

    override fun onOnlinePaymentLimitReached(cart: ShoppingCart) {
    }

    override fun onTaxationChanged(cart: ShoppingCart, taxation: Taxation) {
    }

    override fun onViolationDetected(violations: List<ViolationNotification>) {
    }

    override fun onCartDataChanged(cart: ShoppingCart) {}
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
