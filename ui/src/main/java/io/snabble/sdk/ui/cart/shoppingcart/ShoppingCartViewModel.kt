package io.snabble.sdk.ui.cart.shoppingcart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.PriceFormatter
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.item.ItemType
import io.snabble.sdk.shoppingcart.data.listener.SimpleShoppingCartListener
import io.snabble.sdk.ui.cart.shoppingcart.cartdiscount.model.CartDiscountItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.DepositItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.DepositReturnItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.DiscountItem
import io.snabble.sdk.ui.cart.shoppingcart.product.model.ProductItem
import io.snabble.sdk.ui.telemetry.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShoppingCartViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var currentCart: ShoppingCart? = null
    private var priceFormatter: PriceFormatter? = null
    private var project: Project? = null

    private val simpleShoppingCartListener = object : SimpleShoppingCartListener() {
        override fun onChanged(cart: ShoppingCart) {
            currentCart = cart
            updateUiState(cart)
        }
    }

    // used to update the cart remote -> do not delete it
    fun updateCart() {
        currentCart?.let { updateUiState(it) }
    }

    init {
        project = Snabble.checkedInProject.value
        project?.let {
            priceFormatter = PriceFormatter(it)
            updateCart(it.shoppingCart)
            updateOnShoppingCartChange(it)
        }
        updateOnProjectChange()
    }

    private fun updateOnShoppingCartChange(project: Project) {
        viewModelScope.launch {
            project.shoppingCartFlow.collectLatest {
                updateCart(it)
            }
        }
    }

    private fun updateOnProjectChange() {
        viewModelScope.launch {
            Snabble.checkedInProject.asFlow().filterNotNull().collectLatest {
                priceFormatter = PriceFormatter(it)
                updateCart(it.shoppingCart)
                updateOnShoppingCartChange(it)
            }
        }
    }

    private fun updateCart(shoppingCart: ShoppingCart) {
        currentCart?.removeListener(simpleShoppingCartListener)
        currentCart = shoppingCart
        currentCart?.addListener(simpleShoppingCartListener)
        updateUiState(shoppingCart)
    }

    fun onEvent(event: Event) {
        when (event) {
            is RemoveItem -> removeItemFromCart(event.item, event.onSuccess)
            is UpdateQuantity -> updateQuantity(event.item, event.quantity)
        }
    }

    private fun removeItemFromCart(item: ShoppingCart.Item?, onSuccess: (index: Int) -> Unit) {
        val index = currentCart?.indexOf(item) ?: return
        if (index != -1) {
            currentCart?.remove(index)
            Telemetry.event(Telemetry.Event.DeletedFromCart, item?.product)
            onSuccess(index)
        }
    }

    private fun updateQuantity(item: ShoppingCart.Item, quantity: Int) {
        item.updateQuantity(quantity)
        Telemetry.event(Telemetry.Event.CartAmountChanged, item.product)
    }

    private fun updateUiState(cart: ShoppingCart) {
        val cartItems: MutableList<CartItem> = mutableListOf()
        with(cart.filterNotNull()) {
            filter { it.type == ItemType.PRODUCT }.let { cartItems.addProducts(it) }
            filter { it.type == ItemType.DEPOSIT_RETURN_VOUCHER }.let { cartItems.addDepositReturnItems(it) }

            filter { it.isDiscount && it.lineItem?.discountType != "cart" }.let { cartItems.addDiscountsToProducts(it) }
            filter { it.isDiscount && it.lineItem?.discountType == "cart" }.let { cartItems.addCartDiscount(it) }
        }
        cartItems.addPriceModifiersAsDiscountsProducts()

        cartItems.updatePrices()
        cartItems.sortCartDiscountsToBottom()

        _uiState.update { it.copy(items = cartItems, totalCartPrice = currentCart?.totalPrice) }
    }

    private fun MutableList<CartItem>.addDepositReturnItems(items: List<ShoppingCart.Item>) {
        items.forEach { item ->
            val totalDepositReturnPrice =
                item.depositReturnVoucher?.lineItems?.sumOf { it.totalPrice } ?: return@forEach

            add(
                DepositReturnItem(
                    item = item,
                    totalDeposit = priceFormatter?.format(totalDepositReturnPrice).orEmpty()
                )
            )
        }
    }

    private fun MutableList<CartItem>.sortCartDiscountsToBottom() {
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
                val price = item.calculateTotalPrice()
                // Since the total price can be null as we invalidate the online prices,
                // we need to use the price text instead to display the product price without an changed instead
                val priceText = if (price == 0) item.priceText else priceFormatter?.format(price).orEmpty()

                item.copy(
                    totalPriceText = priceText,
                    discountedPrice = when {
                        item.discounts.isNotEmpty() -> priceFormatter?.format(item.getPriceWithDiscountsApplied())
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
                    totalPrice = item.totalPrice,
                    deposit = item.deposit?.let {
                        DepositItem(
                            depositPrice = item.quantity * it.lineItem.price,
                            depositText = it.lineItem.name,
                            depositPriceText = priceFormatter?.format(item.quantity * it.lineItem.price)
                        )
                    }
                )
            })
    }

    private fun MutableList<CartItem>.addPriceModifiersAsDiscountsProducts() = replaceAll { item ->
        when {
            item is ProductItem && !item.item.lineItem?.priceModifiers.isNullOrEmpty() -> {
                val discounts = mutableListOf<DiscountItem>()
                item.item.lineItem?.priceModifiers?.forEach { priceModifier ->
                    val name = priceModifier.name.orEmpty()
                    val weightUnit = item.item.lineItem?.weightUnit
                    val referenceUnit = item.item.lineItem?.referenceUnit
                    val modifiedPrice = if (weightUnit != null && referenceUnit != null) {
                        priceModifier
                            .convertPriceModifier(
                                amount = item.quantity,
                                weightedUnit = weightUnit,
                                referencedUnit = referenceUnit
                            )
                            .let { priceFormatter?.format(it).orEmpty() }
                    } else {
                        (priceModifier.price * item.quantity).let {
                            priceFormatter?.format(it).orEmpty()
                        }
                    }
                    // Set this to zero because the backend already subtracted the discount from the total price:
                    val discountValue = 0
                    modifiedPrice.let {
                        discounts.add(
                            DiscountItem(
                                name = name,
                                discount = modifiedPrice,
                                discountValue = discountValue
                            )
                        )
                    }
                }
                item.copy(discounts = item.discounts + discounts)
            }

            else -> item
        }
    }

    private fun MutableList<CartItem>.addDiscountsToProducts(discounts: List<ShoppingCart.Item>) {
        discounts.forEach { item ->
            val discount = item.totalPriceText ?: return@forEach
            val name = item.displayName.orEmpty()
            val value = item.totalPrice

            firstOrNull { it.item.id == item.lineItem?.refersTo }
                ?.let {
                    remove(it)
                    val product = it as? ProductItem ?: return@forEach
                    add(product.copy(discounts = it.discounts + DiscountItem(name, discount, value)))
                }
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
    val items: List<CartItem> = emptyList(),
    val totalCartPrice: Int? = null
)
