package io.snabble.sdk.ui.cart

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import io.snabble.sdk.Product
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble.instance
import io.snabble.sdk.Unit
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.item.ItemType
import io.snabble.sdk.shoppingcart.data.listener.ShoppingCartListener
import io.snabble.sdk.shoppingcart.data.listener.SimpleShoppingCartListener
import io.snabble.sdk.ui.GestureHandler
import io.snabble.sdk.ui.GestureHandler.DismissibleAdapter
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.SnabbleUI.executeAction
import io.snabble.sdk.ui.checkout.showNotificationOnce
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.I18nUtils.getIdentifier
import io.snabble.sdk.ui.utils.SnackbarUtils.make
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.observeView
import io.snabble.sdk.utils.Logger
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks

class ShoppingCartView : FrameLayout {

    private var rootView: View? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: ShoppingCartAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var cart: ShoppingCart
    private var emptyState: ViewGroup? = null
    private lateinit var restore: View
    private lateinit var scanProducts: TextView
    private var hasAnyImages = false
    private var lastInvalidProducts: List<Product>? = null
    private var alertDialog: AlertDialog? = null
    private var paymentContainer: View? = null
    private var hasAlreadyShownInvalidDeposit = false
    private var project: Project? = null
    private var isRegistered = false

    private val shoppingCartListener: ShoppingCartListener = object : SimpleShoppingCartListener() {
        override fun onChanged(cart: ShoppingCart?) {
            swipeRefreshLayout!!.isRefreshing = false
            submitList()
            update()
        }

        override fun onCheckoutLimitReached(list: ShoppingCart?) {
            if (alertDialog != null) {
                alertDialog!!.dismiss()
            }

            val message = resources.getString(
                R.string.Snabble_LimitsAlert_checkoutNotAvailable,
                project!!.priceFormatter.format(project!!.maxCheckoutLimit)
            )

            alertDialog = AlertDialog.Builder(context)
                .setTitle(R.string.Snabble_LimitsAlert_title)
                .setMessage(message)
                .setPositiveButton(R.string.Snabble_ok, null)
                .create()
            alertDialog!!.show()
        }

        override fun onOnlinePaymentLimitReached(list: ShoppingCart?) {
            if (alertDialog != null) {
                alertDialog!!.dismiss()
            }

            val message = resources.getString(
                R.string.Snabble_LimitsAlert_notAllMethodsAvailable,
                project!!.priceFormatter.format(project!!.maxOnlinePaymentLimit)
            )

            alertDialog = AlertDialog.Builder(context)
                .setTitle(R.string.Snabble_LimitsAlert_title)
                .setMessage(message)
                .setPositiveButton(R.string.Snabble_ok, null)
                .create()
            alertDialog!!.show()
        }

        override fun onViolationDetected(violations: List<ViolationNotification?>) {
            violations.showNotificationOnce(context, cart)
        }
    }

    constructor(context: Context) : super(context) {
        inflateView(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflateView(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflateView(context, attrs)
    }

    private fun inflateView(context: Context, attrs: AttributeSet?) {
        inflate(getContext(), R.layout.snabble_view_shopping_cart, this)

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
            rootView!!.visibility = VISIBLE
            unregisterListeners()
            project = p

            if (cart != null) {
                cart!!.removeListener(shoppingCartListener)
            }

            cart = project!!.shoppingCart
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
            recyclerView.getContext(),
            layoutManager.orientation
        )
        recyclerView.addItemDecoration(itemDecoration)

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        swipeRefreshLayout.setOnRefreshListener(OnRefreshListener { cart!!.updatePrices(false) })

        emptyState = findViewById(R.id.empty_state)

        paymentContainer = findViewById(R.id.bottom_payment_container)

        scanProducts = findViewById(R.id.scan_products)
        scanProducts.setOnClickListener(OnClickListener { view: View? ->
            executeAction(
                context,
                SnabbleUI.Event.SHOW_SCANNER
            )
        })

        restore = findViewById(R.id.restore)
        restore.setOnClickListener(OnClickListener { v: View? -> cart!!.restore() })

        PaymentSelectionHelper
            .getInstance()
            .selectedEntry
            .observe((UIUtils.getHostActivity(getContext()) as FragmentActivity)) { entry: PaymentSelectionHelper.Entry? -> update() }

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
                val item = cart!![pos]
                recyclerViewAdapter!!.removeAndShowUndoSnackbar(pos, item)
            }
        }
        val itemTouchHelper = ItemTouchHelper(gestureHandler)
        gestureHandler.setItemTouchHelper(itemTouchHelper)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun updateEmptyState() {
        if (cart!!.size() > 0) {
            paymentContainer!!.visibility = VISIBLE
            emptyState!!.visibility = GONE
        } else {
            paymentContainer!!.visibility = GONE
            emptyState!!.visibility = VISIBLE
        }

        if (cart!!.isRestorable) {
            restore!!.visibility = VISIBLE
            scanProducts!!.setText(R.string.Snabble_Shoppingcart_EmptyState_restartButtonTitle)
        } else {
            restore!!.visibility = GONE
            scanProducts!!.setText(R.string.Snabble_Shoppingcart_EmptyState_buttonTitle)
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
        val invalidProducts = cart!!.invalidProducts

        if (invalidProducts!!.size > 0 && invalidProducts != lastInvalidProducts) {
            val res = resources
            val sb = StringBuilder()
            if (invalidProducts.size == 1) {
                sb.append(res.getString(getIdentifier(res, R.string.Snabble_SaleStop_ErrorMsg_one)))
            } else {
                sb.append(res.getString(getIdentifier(res, R.string.Snabble_SaleStop_errorMsg)))
            }

            sb.append("\n\n")

            for (product in invalidProducts) {
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
        if (cart!!.hasInvalidDepositReturnVoucher() && !hasAlreadyShownInvalidDeposit) {
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

        hasAnyImages = false

        for (i in 0 until cart!!.size()) {
            val item = cart!![i]
            if (item.type != ItemType.PRODUCT) continue

            val product = item.product
            val url = product!!.imageUrl
            if (url != null && url.length > 0) {
                hasAnyImages = true
                break
            }
        }

        if (hasAnyImages != lastHasAnyImages) {
            submitList()
            update()
        }
    }

    private fun registerListeners() {
        if (!isRegistered && project != null) {
            isRegistered = true
            cart!!.addListener(shoppingCartListener)
            submitList()
            update()
        }
    }

    private fun unregisterListeners() {
        if (isRegistered) {
            isRegistered = false
            cart!!.removeListener(shoppingCartListener)
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
            if (UIUtils.getHostActivity(context) === activity) {
                registerListeners()

                submitList()
                update()
            }
        }

        override fun onActivityStopped(activity: Activity) {
            if (UIUtils.getHostActivity(context) === activity) {
                unregisterListeners()
            }
        }
    }

    private fun submitList() {
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter!!.submitList(buildRows(resources, cart), hasAnyImages)
        }
    }

    abstract class Row {

        var item: ShoppingCart.Item? = null
        var isDismissible: Boolean = false

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false

            val row = o as Row

            if (isDismissible != row.isDismissible) return false
            return if (item != null) item == row.item else row.item == null
        }

        override fun hashCode(): Int {
            var result = if (item != null) item.hashCode() else 0
            result = 31 * result + (if (isDismissible) 1 else 0)
            return result
        }
    }

    class ProductRow : Row() {

        var name: String? = null
        var subtitle: String? = null
        var imageUrl: String? = null
        var encodingUnit: Unit? = null
        var priceText: String? = null
        var quantityText: String? = null
        var quantity: Int = 0
        var editable: Boolean = false
        var manualDiscountApplied: Boolean = false

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            if (!super.equals(o)) return false

            val that = o as ProductRow

            if (quantity != that.quantity) return false
            if (editable != that.editable) return false
            if (manualDiscountApplied != that.manualDiscountApplied) return false
            if (if (name != null) name != that.name else that.name != null) return false
            if (if (subtitle != null) subtitle != that.subtitle else that.subtitle != null) return false
            if (if (imageUrl != null) imageUrl != that.imageUrl else that.imageUrl != null) return false
            if (encodingUnit != that.encodingUnit) return false
            if (if (priceText != null) priceText != that.priceText else that.priceText != null) return false
            return if (quantityText != null) quantityText == that.quantityText else that.quantityText == null
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + (if (name != null) name.hashCode() else 0)
            result = 31 * result + (if (subtitle != null) subtitle.hashCode() else 0)
            result = 31 * result + (if (imageUrl != null) imageUrl.hashCode() else 0)
            result = 31 * result + (if (encodingUnit != null) encodingUnit.hashCode() else 0)
            result = 31 * result + (if (priceText != null) priceText.hashCode() else 0)
            result = 31 * result + (if (quantityText != null) quantityText.hashCode() else 0)
            result = 31 * result + quantity
            result = 31 * result + (if (editable) 1 else 0)
            result = 31 * result + (if (manualDiscountApplied) 1 else 0)
            return result
        }
    }

    class SimpleRow : Row() {

        var text: String? = null
        var title: String? = null

        @DrawableRes
        var imageResId: Int = 0

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            if (!super.equals(o)) return false

            val simpleRow = o as SimpleRow

            if (imageResId != simpleRow.imageResId) return false
            if (if (item != null) item != simpleRow.item else simpleRow.item != null) return false
            if (if (text != null) text != simpleRow.text else simpleRow.text != null) return false
            return if (title != null) title == simpleRow.title else simpleRow.title == null
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + (if (item != null) item.hashCode() else 0)
            result = 31 * result + (if (text != null) text.hashCode() else 0)
            result = 31 * result + (if (title != null) title.hashCode() else 0)
            result = 31 * result + imageResId
            return result
        }
    }

    class SimpleViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var title: TextView = itemView.findViewById(R.id.title)
        var text: TextView
        var image: ImageView

        init {
            text = itemView.findViewById(R.id.text)
            image = itemView.findViewById(R.id.helper_image)
        }

        fun update(row: SimpleRow, hasAnyImages: Boolean) {
            title.text = row.title
            text.text = row.text
            image.setImageResource(row.imageResId)

            if (hasAnyImages) {
                image.visibility = VISIBLE
            } else {
                image.visibility = GONE
            }
        }
    }

    class ShoppingCartAdapter(private val parentView: View?, private val cart: ShoppingCart?) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>(), UndoHelper, DismissibleAdapter {

        private var list = emptyList<Row>()
        private val context: Context = parentView!!.context
        private var hasAnyImages = false

        override fun getItemViewType(position: Int): Int {
            if (getItem(position) is SimpleRow) {
                return TYPE_SIMPLE
            }

            return TYPE_PRODUCT
        }

        override fun isDismissible(position: Int): Boolean {
            return getItem(position).isDismissible
        }

        override fun getItemCount(): Int {
            return list.size
        }

        // for fetching the data from outside of this view
        fun fetchFrom(cart: ShoppingCart?) {
            hasAnyImages = false

            for (i in 0 until cart!!.size()) {
                val item = cart[i]
                if (item.type != ItemType.PRODUCT) continue

                val product = item.product
                val url = product!!.imageUrl
                if (url != null && url.length > 0) {
                    hasAnyImages = true
                    break
                }
            }

            submitList(buildRows(context.resources, cart), hasAnyImages)
        }

        fun submitList(newList: List<Row>, hasAnyImages: Boolean) {
            this.hasAnyImages = hasAnyImages
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return list.size
                }

                override fun getNewListSize(): Int {
                    return newList.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldRow = list[oldItemPosition]
                    val newRow = newList[newItemPosition]

                    if (oldRow.item == null || newRow.item == null) {
                        return false
                    }

                    return oldRow.item == newRow.item
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val oldRow = list[oldItemPosition]
                    val newRow = newList[newItemPosition]

                    return oldRow == newRow
                }
            })

            list = newList
            diffResult.dispatchUpdatesTo(this)
        }

        fun getItem(position: Int): Row {
            return list[position]
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            if (viewType == TYPE_SIMPLE) {
                val v = inflate(context, R.layout.snabble_item_shoppingcart_simple, null)
                v.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                return SimpleViewHolder(v)
            } else {
                val v = inflate(context, R.layout.snabble_item_shoppingcart_product, null)
                v.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                return ShoppingCartItemViewHolder(v, this)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val type = getItemViewType(position)

            if (type == TYPE_PRODUCT) {
                val viewHolder = holder as ShoppingCartItemViewHolder
                viewHolder.bindTo((getItem(position) as ProductRow), hasAnyImages)
            } else {
                val viewHolder = holder as SimpleViewHolder
                viewHolder.update(getItem(position) as SimpleRow, hasAnyImages)
            }
        }

        override fun removeAndShowUndoSnackbar(adapterPosition: Int, item: ShoppingCart.Item) {
            if (adapterPosition == -1) {
                Logger.d("Invalid adapter position, ignoring")
                return
            }

            cart!!.remove(adapterPosition)
            Telemetry.event(Telemetry.Event.DeletedFromCart, item.product)

            val snackbar = make(
                parentView!!,
                R.string.Snabble_Shoppingcart_articleRemoved, UIUtils.SNACKBAR_LENGTH_VERY_LONG
            )
            snackbar.setAction(R.string.Snabble_undo) { v: View? ->
                cart.insert(item, adapterPosition)
                fetchFrom(cart)
                Telemetry.event(Telemetry.Event.UndoDeleteFromCart, item.product)
            }
            snackbar.show()
            fetchFrom(cart)
        }

        companion object {

            private const val TYPE_PRODUCT = 0
            private const val TYPE_SIMPLE = 1
        }
    }

    companion object {

        private fun sanitize(input: String?): String? {
            if (input != null && input == "") return null
            return input
        }

        fun buildRows(resources: Resources, cart: ShoppingCart?): List<Row> {
            val rows: MutableList<Row> = ArrayList(
                cart!!.size() + 1
            )

            for (i in 0 until cart.size()) {
                val item = cart[i]

                if (item.type == ItemType.LINE_ITEM) {
                    if (item.isDiscount) {
                        val row = SimpleRow()
                        row.item = item
                        row.title = resources.getString(R.string.Snabble_Shoppingcart_discounts)
                        row.imageResId = R.drawable.snabble_ic_percent
                        row.text = sanitize(item.priceText)
                        rows.add(row)
                    } else if (item.isGiveaway) {
                        val row = SimpleRow()
                        row.item = item
                        row.title = item.displayName
                        row.imageResId = R.drawable.snabble_ic_gift
                        row.text = resources.getString(R.string.Snabble_Shoppingcart_giveaway)
                        rows.add(row)
                    }
                } else if (item.type == ItemType.COUPON) {
                    val row = SimpleRow()
                    row.item = item
                    row.title = resources.getString(R.string.Snabble_Shoppingcart_coupon)
                    row.text = item.displayName
                    row.isDismissible = true
                    rows.add(row)
                } else if (item.type == ItemType.PRODUCT) {
                    val row = ProductRow()
                    val product = item.product
                    val quantity = item.getQuantityMethod()

                    if (product != null) {
                        row.subtitle = sanitize(product.subtitle)
                        row.imageUrl = sanitize(product.imageUrl)
                    }

                    row.name = sanitize(item.displayName)
                    row.encodingUnit = item.unit
                    row.priceText = sanitize(item.totalPriceText)
                    row.quantity = quantity
                    row.quantityText = sanitize(item.quantityText)
                    row.editable = item.isEditable
                    row.isDismissible = true
                    row.manualDiscountApplied = item.isManualCouponApplied
                    row.item = item
                    rows.add(row)
                }
            }

            val cartTotal = cart.totalDepositPrice
            if (cartTotal > 0) {
                val row = SimpleRow()
                val priceFormatter = instance.checkedInProject.getValue()!!.priceFormatter
                row.title = resources.getString(R.string.Snabble_Shoppingcart_deposit)
                row.imageResId = R.drawable.snabble_ic_deposit
                row.text = priceFormatter.format(cartTotal)
                rows.add(row)
            }

            return rows
        }
    }
}
