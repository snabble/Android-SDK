package io.snabble.sdk.ui.cart.shoppingcart

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.snabble.sdk.Product
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble.instance
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.item.ItemType
import io.snabble.sdk.shoppingcart.data.listener.ShoppingCartListener
import io.snabble.sdk.shoppingcart.data.listener.SimpleShoppingCartListener
import io.snabble.sdk.ui.GestureHandler
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.SnabbleUI.executeAction
import io.snabble.sdk.ui.cart.PaymentSelectionHelper
import io.snabble.sdk.ui.cart.ShoppingCartItemViewHolder
import io.snabble.sdk.ui.cart.shoppingcart.adapter.ShoppingCartAdapter
import io.snabble.sdk.ui.cart.shoppingcart.row.ProductRow
import io.snabble.sdk.ui.cart.shoppingcart.row.Row
import io.snabble.sdk.ui.cart.shoppingcart.row.SimpleRow
import io.snabble.sdk.ui.checkout.showNotificationOnce
import io.snabble.sdk.ui.utils.I18nUtils.getIdentifier
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.observeView
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks

class ShoppingCartView : FrameLayout {

    private var rootView: View? = null
    private lateinit var recyclerView: RecyclerView
    private var recyclerViewAdapter: ShoppingCartAdapter? = null
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
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
        override fun onChanged(list: ShoppingCart?) {
            swipeRefreshLayout.isRefreshing = false
            submitList()
            update()
        }

        override fun onCheckoutLimitReached(list: ShoppingCart?) {
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

        override fun onOnlinePaymentLimitReached(list: ShoppingCart?) {
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

        override fun onViolationDetected(violations: List<ViolationNotification?>) {
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

        instance.checkedInProject.observeView(this) { p: Project? ->
            if (p != null) {
                initViewState(p)
            }
        }

        val currentProject = instance.checkedInProject.getValue()
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
        recyclerView = findViewById(R.id.recycler_view)
        recyclerViewAdapter = ShoppingCartAdapter(recyclerView, cart)
        recyclerView.setAdapter(recyclerViewAdapter)

        val layoutManager = LinearLayoutManager(context)
        recyclerView.setLayoutManager(layoutManager)
        val itemAnimator = DefaultItemAnimator()
        itemAnimator.supportsChangeAnimations = false
        recyclerView.setItemAnimator(itemAnimator)

        val itemDecoration = DividerItemDecoration(
            recyclerView.context,
            layoutManager.orientation
        )
        recyclerView.addItemDecoration(itemDecoration)

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

        createItemTouchHelper(context.resources)
        submitList()
        update()
    }

    private fun createItemTouchHelper(resources: Resources) {
        val gestureHandler: GestureHandler<Void?> = object : GestureHandler<Void?>(resources) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (viewHolder is ShoppingCartItemViewHolder) {
                    viewHolder.hideInput()
                }

                val pos = viewHolder.bindingAdapterPosition
                val item = cart?.get(pos)
                item?.let {
                    recyclerViewAdapter?.removeAndShowUndoSnackbar(pos, item)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(gestureHandler)
        gestureHandler.setItemTouchHelper(itemTouchHelper)
        itemTouchHelper.attachToRecyclerView(recyclerView)
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
            checkDepositReturnVoucher()
        }
    }

    private fun checkSaleStop() {
        val invalidProducts = cart?.invalidProducts

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
        }
    }

    private fun checkDepositReturnVoucher() {
        if (cart?.hasInvalidDepositReturnVoucher() == true && !hasAlreadyShownInvalidDeposit) {
            AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(getIdentifier(resources, R.string.Snabble_SaleStop_ErrorMsg_title))
                .setMessage(getIdentifier(resources, R.string.Snabble_InvalidDepositVoucher_errorMsg))
                .setPositiveButton(R.string.Snabble_ok, null)
                .show()
            hasAlreadyShownInvalidDeposit = true
        }
    }

    private fun scanForImages() {
        val lastHasAnyImages = hasAnyImages

        hasAnyImages = cart?.any { !it?.product?.imageUrl.isNullOrEmpty() } == true

        if (hasAnyImages != lastHasAnyImages) {
            submitList()
            update()
        }
    }

    private fun registerListeners() {
        if (!isRegistered && project != null) {
            isRegistered = true
            cart?.addListener(shoppingCartListener)
            submitList()
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

        submitList()
        update()
    }

    private val activityLifecycleCallbacks: ActivityLifecycleCallbacks = object : SimpleActivityLifecycleCallbacks() {
        override fun onActivityStarted(activity: Activity) {
            if (UIUtils.getHostActivity(context) == activity) {
                registerListeners()

                submitList()
                update()
            }
        }

        override fun onActivityStopped(activity: Activity) {
            if (UIUtils.getHostActivity(context) == activity) {
                unregisterListeners()
            }
        }
    }

    private fun submitList() {
        cart?.let {
            recyclerViewAdapter?.submitList(buildRows(resources, it), hasAnyImages)
        }
    }

    companion object {

        private fun sanitize(input: String?): String? {
            if (input != null && input.isBlank()) return null
            return input
        }

        fun buildRows(resources: Resources, cart: ShoppingCart): List<Row> {
            val rows: MutableList<Row> = ArrayList(
                cart.size() + 1
            )
            cart.forEach { item: ShoppingCart.Item? ->
                if (item?.type == ItemType.LINE_ITEM) {
                    if (item.isDiscount) {
                        val row = SimpleRow(
                            item = item,
                            title = resources.getString(R.string.Snabble_Shoppingcart_discounts),
                            imageResId = R.drawable.snabble_ic_percent,
                            text = sanitize(item.priceText)
                        )
                        rows.add(row)
                    } else if (item.isGiveaway) {
                        val row = SimpleRow(
                            item = item,
                            title = item.displayName,
                            imageResId = R.drawable.snabble_ic_gift,
                            text = resources.getString(R.string.Snabble_Shoppingcart_giveaway)
                        )
                        rows.add(row)
                    }
                } else if (item?.type == ItemType.COUPON) {
                    val row = SimpleRow(
                        item = item,
                        title = resources.getString(R.string.Snabble_Shoppingcart_coupon),
                        text = item.displayName,
                        isDismissible = true
                    )
                    rows.add(row)
                } else if (item?.type == ItemType.PRODUCT) {
                    val product = item.product
                    val quantity = item.getQuantityMethod()

                    val row = ProductRow(
                        subtitle = sanitize(product?.subtitle),
                        imageUrl = sanitize(product?.imageUrl),
                        name = sanitize(item.displayName),
                        encodingUnit = item.unit,
                        priceText = sanitize(item.totalPriceText),
                        quantity = quantity,
                        quantityText = sanitize(item.quantityText),
                        editable = item.isEditable,
                        isDismissible = true,
                        manualDiscountApplied = item.isManualCouponApplied,
                        item = item
                    )

                    rows.add(row)
                }
            }

            val cartTotal = cart.totalDepositPrice
            if (cartTotal > 0) {
                val priceFormatter = instance.checkedInProject.getValue()?.priceFormatter
                val row = SimpleRow(
                    title = resources.getString(R.string.Snabble_Shoppingcart_deposit),
                    imageResId = R.drawable.snabble_ic_deposit,
                    text = priceFormatter?.format(cartTotal)
                )
                rows.add(row)
            }

            return rows
        }
    }
}
