package io.snabble.sdk.ui.cart.deprecated

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.snabble.sdk.Product
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.listener.ShoppingCartListener
import io.snabble.sdk.shoppingcart.data.listener.SimpleShoppingCartListener
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.SnabbleUI.executeAction
import io.snabble.sdk.ui.cart.PaymentSelectionHelper
import io.snabble.sdk.ui.cart.shoppingcart.ShoppingCartScreen
import io.snabble.sdk.ui.cart.showInvalidProductsDialog
import io.snabble.sdk.ui.checkout.showNotificationOnce
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.I18nUtils.getIdentifier
import io.snabble.sdk.ui.utils.SnackbarUtils.make
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.isNotNullOrBlank
import io.snabble.sdk.ui.utils.observeView
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks

@Deprecated("Integrate [io.snabble.sdk.ui.cart.shoppingcart.ShoppingCartScreen] instead.")
class ShoppingCartView : FrameLayout {

    private var rootView: View? = null
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var composeView: ComposeView
    private var cart: ShoppingCart? = null
    private var emptyState: ViewGroup? = null
    private var restore: View? = null
    private var scanProducts: TextView? = null
    private var hasAnyImages = false
    private var lastInvalidProducts: List<Product>? = null
    private var alertDialog: AlertDialog? = null
    private var paymentContainer: View? = null
    private var hasAlreadyShownInvalidDeposit = false
    private var project: Project? = null
    private var isRegistered = false

    private val shoppingCartListener: ShoppingCartListener = object : SimpleShoppingCartListener() {
        override fun onChanged(cart: ShoppingCart) {
            swipeRefreshLayout.isRefreshing = false
            update()
        }

        override fun onCheckoutLimitReached(cart: ShoppingCart) {
            alertDialog?.dismiss()

            project?.let {

                val message = resources.getString(
                    R.string.Snabble_LimitsAlert_checkoutNotAvailable,
                    it.priceFormatter.format(it.maxCheckoutLimit)
                )

                alertDialog = AlertDialog.Builder(context)
                    .setTitle(R.string.Snabble_LimitsAlert_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.Snabble_ok, null)
                    .create()
                alertDialog?.show()
            }
        }

        override fun onOnlinePaymentLimitReached(cart: ShoppingCart) {
            alertDialog?.dismiss()
            project?.let {

                val message = resources.getString(
                    R.string.Snabble_LimitsAlert_notAllMethodsAvailable,
                    it.priceFormatter.format(it.maxOnlinePaymentLimit)
                )

                alertDialog = AlertDialog.Builder(context)
                    .setTitle(R.string.Snabble_LimitsAlert_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.Snabble_ok, null)
                    .create()
                alertDialog?.show()
            }
        }

        override fun onViolationDetected(violations: List<ViolationNotification>) {
            cart?.let {
                violations.showNotificationOnce(context, it)
            }
        }
    }

    constructor(context: Context) : super(context) {
        inflateView(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflateView(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflateView(context)
    }

    private fun inflateView(context: Context) {
        inflate(context, R.layout.snabble_view_shopping_cart, this)

        rootView = findViewById(R.id.root)

        if (isInEditMode) return

        Snabble.instance.checkedInProject.observeView(this) { p: Project? ->
            if (p != null) {
                initViewState(p)
            }
        }

        val currentProject = Snabble.instance.checkedInProject.getValue()
        if (currentProject != null) {
            initViewState(currentProject)
        }
    }

    private fun initViewState(p: Project) {
        if (p != project) {
            rootView?.visibility = VISIBLE
            unregisterListeners()
            project = p

            cart?.removeListener(shoppingCartListener)


            cart = project?.shoppingCart
            resetViewState(context)
            registerListeners()
        }
    }

    private fun resetViewState(context: Context) {
        composeView = findViewById(R.id.compose_card_container)

        composeView.setContent {
            ShoppingCartScreen(
                onItemDeleted = { item, index ->
                    make(this, R.string.Snabble_Shoppingcart_articleRemoved, UIUtils.SNACKBAR_LENGTH_VERY_LONG)
                        .setAction(R.string.Snabble_undo) {
                            cart?.insert(item, index)
                            Telemetry.event(Telemetry.Event.UndoDeleteFromCart, item.product)
                        }
                        .show()
                }
            )
        }

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener { cart?.updatePrices(false) }

        emptyState = findViewById(R.id.empty_state)

        paymentContainer = findViewById(R.id.bottom_payment_container)

        scanProducts = findViewById(R.id.scan_products)
        scanProducts?.setOnClickListener { executeAction(context, SnabbleUI.Event.SHOW_SCANNER) }

        restore = findViewById(R.id.restore)
        restore?.setOnClickListener { cart?.restore() }

        PaymentSelectionHelper.getInstance()
            .selectedEntry
            .observe((UIUtils.getHostActivity(getContext()) as FragmentActivity)) { update() }

        update()
    }

    private fun updateEmptyState() {
        if (cart?.isEmpty != true) {
            paymentContainer?.visibility = VISIBLE
            emptyState?.visibility = GONE
        } else {
            paymentContainer?.visibility = GONE
            emptyState?.visibility = VISIBLE
        }

        if (cart?.isRestorable == true) {
            restore?.visibility = VISIBLE
            scanProducts?.setText(R.string.Snabble_Shoppingcart_EmptyState_restartButtonTitle)
        } else {
            restore?.visibility = GONE
            scanProducts?.setText(R.string.Snabble_Shoppingcart_EmptyState_buttonTitle)
        }
    }

    private fun update() {
        if (project != null) {
            updateEmptyState()
            scanForImages()
            checkSaleStop()
        }
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
            val invalidItems = cart?.mapNotNull { it }?.filter { it.id in invalidItemIds } ?: return

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

    private fun scanForImages() {
        val lastHasAnyImages = hasAnyImages

        hasAnyImages = cart?.any { it?.product?.imageUrl.isNotNullOrBlank() } == true

        if (hasAnyImages != lastHasAnyImages) {
            update()
        }
    }

    private fun registerListeners() {
        if (!isRegistered && project != null) {
            isRegistered = true
            cart?.addListener(shoppingCartListener)
            update()
        }
    }

    private fun unregisterListeners() {
        if (isRegistered) {
            isRegistered = false
            cart?.removeListener(shoppingCartListener)
        }
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!isInEditMode) {
            val application = context.applicationContext as Application
            application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)

            registerListeners()
        }
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        val application = context.applicationContext as Application
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)

        unregisterListeners()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)

        update()
    }

    private val activityLifecycleCallbacks: ActivityLifecycleCallbacks = object : SimpleActivityLifecycleCallbacks() {
        override fun onActivityStarted(activity: Activity) {
            if (UIUtils.getHostActivity(context) == activity) {
                registerListeners()

                update()
            }
        }

        override fun onActivityStopped(activity: Activity) {
            if (UIUtils.getHostActivity(context) == activity) {
                unregisterListeners()
            }
        }
    }
}
