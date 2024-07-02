package io.snabble.sdk.ui.scanner.combined

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.compose.ui.platform.ComposeView
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.Taxation
import io.snabble.sdk.shoppingcart.data.listener.ShoppingCartListener
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.CheckoutBar
import io.snabble.sdk.ui.cart.shoppingcart.ShoppingCartScreen
import io.snabble.sdk.ui.telemetry.Telemetry
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

    override fun onItemAdded(list: ShoppingCart?, item: ShoppingCart.Item?) {
        onItemsChangedListener.forEach { it.invoke(requireNotNull(cart)) }
    }

    override fun onQuantityChanged(list: ShoppingCart?, item: ShoppingCart.Item?) {}

    override fun onCleared(list: ShoppingCart?) {
        onItemsChangedListener.forEach { it.invoke(requireNotNull(cart)) }
    }

    override fun onItemRemoved(list: ShoppingCart?, item: ShoppingCart.Item?, pos: Int) {
        onItemsChangedListener.forEach { it.invoke(requireNotNull(cart)) }
    }

    override fun onProductsUpdated(list: ShoppingCart?) {}
    override fun onPricesUpdated(list: ShoppingCart?) {}
    override fun onCheckoutLimitReached(list: ShoppingCart?) {}
    override fun onOnlinePaymentLimitReached(list: ShoppingCart?) {}
    override fun onTaxationChanged(list: ShoppingCart?, taxation: Taxation?) {}
    override fun onViolationDetected(violations: List<ViolationNotification?>) {}
    override fun onCartDataChanged(list: ShoppingCart?) {}
}
