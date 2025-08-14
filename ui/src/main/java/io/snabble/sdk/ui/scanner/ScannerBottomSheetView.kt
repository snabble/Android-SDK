package io.snabble.sdk.ui.scanner

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import io.snabble.sdk.Product
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.Taxation
import io.snabble.sdk.shoppingcart.data.listener.ShoppingCartListener
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.CheckoutBar
import io.snabble.sdk.ui.cart.shoppingcart.ShoppingCartScreen
import io.snabble.sdk.ui.cart.showInvalidProductsDialog
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.I18nUtils.getIdentifier
import io.snabble.sdk.ui.utils.SnackbarUtils.make
import io.snabble.sdk.ui.utils.UIUtils

class ScannerBottomSheetView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ShoppingCartListener {

    val checkout: CheckoutBar
    private var composeView: ComposeView
    val onItemsChangedListener: MutableList<(cart: ShoppingCart) -> Unit> = mutableListOf()

    val peekHeight: Int
        get() = if (cart?.isRestorable == true || cart?.isEmpty == false) {
            checkout.height
        } else {
            checkout.priceHeight
        }
    private var lastInvalidProducts: List<Product>? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.snabble_view_cart, this)
        checkout = findViewById(R.id.checkout)
        composeView = findViewById(R.id.compose_card_container)
        composeView.setContent {
            ShoppingCartScreen(
                onItemDeleted = { item, index ->
                    val snackbar = make(
                        this, R.string.Snabble_Shoppingcart_articleRemoved, UIUtils.SNACKBAR_LENGTH_VERY_LONG
                    )
                    snackbar.setAction(R.string.Snabble_undo) { v: View? ->
                        cart?.insert(item, index)
                        Telemetry.event(Telemetry.Event.UndoDeleteFromCart, item.product)
                    }
                    snackbar.show()
                }
            )
        }

        orientation = VERTICAL
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cart?.removeListener(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        cart?.addListener(this)
    }

    var cart: ShoppingCart? = null
        set(value) {
            field = value
            value?.addListener(this)
        }

    private fun checkSaleStop() {
        val invalidProducts = cart?.invalidProducts
        val invalidItemIds = cart?.invalidItemIds

        if (invalidProducts?.isNotEmpty() == true && invalidProducts != lastInvalidProducts) {
            val res = resources
            val sb = StringBuilder()
            if (invalidProducts.size == 1) {
                sb.append(res.getString(getIdentifier(res, R.string.Snabble_SaleStop_ErrorMsg_one)))
            } else {
                sb.append(res.getString(getIdentifier(res, R.string.Snabble_SaleStop_errorMsg)))
            }

            sb.append("\n\n")

            invalidProducts.forEach { product ->
                if (product.subtitle != null) {
                    sb.append(product.subtitle)
                    sb.append(" ")
                }

                sb.append(product.name)
                sb.append("\n")
            }

            AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(getIdentifier(resources, R.string.Snabble_SaleStop_ErrorMsg_title))
                .setMessage(sb.toString())
                .setPositiveButton(R.string.Snabble_ok, null)
                .show()

            lastInvalidProducts = invalidProducts
        } else if (!invalidItemIds.isNullOrEmpty()) {
            val invalidItems = cart?.filterNotNull()?.filter { it.id in invalidItemIds } ?: return

            context.showInvalidProductsDialog(
                invalidItems = invalidItems,
                onRemove = {
                    cart?.let { cart ->
                        invalidItems.forEach { item ->
                            val index = cart.indexOf(item)
                            if (index != -1) {
                                cart.remove(index)
                            }
                        }
                    }
                }
            )
        }
    }

    override fun onItemAdded(cart: ShoppingCart, item: ShoppingCart.Item) {
        onItemsChangedListener.forEach { it.invoke(requireNotNull(this.cart)) }
    }

    override fun onQuantityChanged(cart: ShoppingCart, item: ShoppingCart.Item) {}

    override fun onCleared(cart: ShoppingCart) {
        onItemsChangedListener.forEach { it.invoke(requireNotNull(this.cart)) }
    }

    override fun onItemRemoved(cart: ShoppingCart, item: ShoppingCart.Item, pos: Int) {
        onItemsChangedListener.forEach { it.invoke(requireNotNull(this.cart)) }
    }

    override fun onProductsUpdated(cart: ShoppingCart) {}
    override fun onPricesUpdated(cart: ShoppingCart) = checkSaleStop()
    override fun onCheckoutLimitReached(cart: ShoppingCart) {}
    override fun onOnlinePaymentLimitReached(cart: ShoppingCart) {}
    override fun onTaxationChanged(cart: ShoppingCart, taxation: Taxation) {}
    override fun onViolationDetected(violations: List<ViolationNotification>) {}
    override fun onCartDataChanged(cart: ShoppingCart) {}
}
