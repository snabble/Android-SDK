package io.snabble.sdk.ui.scanner

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.accessibility
import io.snabble.sdk.ui.cart.CheckoutBar
import io.snabble.sdk.ui.cart.ShoppingCartView
import io.snabble.sdk.ui.isTalkBackActive
import io.snabble.sdk.ui.utils.behavior


class ScannerBottomSheetView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ShoppingCart.ShoppingCartListener {
    val checkout: CheckoutBar
    val recyclerView: RecyclerView
    val onItemsChangedListener: MutableList<(cart: ShoppingCart) -> Unit> = mutableListOf()

    var shoppingCartAdapter: ShoppingCartView.ShoppingCartAdapter? = null
        set(value) {
            field = value
            if (cart != null) {
                value?.fetchFrom(cart)
            }
            recyclerView.adapter = value
        }

    val peekHeight: Int
        get() = if (cart?.isRestorable == true || cart?.isEmpty == false) {
                    checkout.height
                } else {
                    checkout.priceHeight
                }

    init {
        LayoutInflater.from(context).inflate(R.layout.snabble_view_cart, this)
        checkout = findViewById(R.id.checkout)
        recyclerView = findViewById(R.id.recycler_view)

        orientation = VERTICAL

        recyclerView.layoutManager = LinearLayoutManager(context)
        val animator = DefaultItemAnimator()
        animator.supportsChangeAnimations = false
        recyclerView.itemAnimator = animator
        recyclerView.adapter = shoppingCartAdapter

        val dividerItemDecoration = DividerItemDecoration(recyclerView.context, RecyclerView.VERTICAL)
        recyclerView.addItemDecoration(dividerItemDecoration)

        recyclerView.accessibility {
            onInitializeAccessibilityNodeInfo { info ->
                val rowsCount = recyclerView.adapter?.itemCount ?: 0
                val selectionMode = AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_NONE
                val collectionInfo = AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(rowsCount, 1, false, selectionMode)
                info.setCollectionInfo(collectionInfo)
            }
        }

        if (context.isTalkBackActive) {
            recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    super.getItemOffsets(outRect, view, parent, state)
                    val viewHolder = recyclerView.findContainingViewHolder(view)
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                    val behavior = this@ScannerBottomSheetView.behavior as? BottomSheetBehavior
                    if (viewHolder != null && layoutManager != null && behavior != null) {
                        view.accessibility {
                            onAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) { _, _, _ ->
                                val pos = viewHolder.absoluteAdapterPosition
                                // maximize cart when at second element
                                if (pos >= 1 && behavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                                } else if (pos == 0 && behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                                    behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
                                }
                                // make sure that you can always see the element before and after the current one
                                if (pos == layoutManager.findFirstCompletelyVisibleItemPosition() && pos > 0) {
                                    recyclerView.smoothScrollToPosition(pos - 1)
                                } else if(pos == layoutManager.findLastCompletelyVisibleItemPosition() && pos < recyclerView.adapter!!.itemCount) {
                                    recyclerView.smoothScrollToPosition(pos + 1)
                                }
                            }
                        }
                    }
                }
            })
        }
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
            if (value != null) {
                shoppingCartAdapter?.fetchFrom(cart)
            }
            value?.addListener(this)
        }

    private fun update() {
        shoppingCartAdapter?.fetchFrom(cart)
    }

    override fun onItemAdded(list: ShoppingCart?, item: ShoppingCart.Item?) {
        update()

        if (item?.type == ShoppingCart.ItemType.PRODUCT) {
            recyclerView.scrollToPosition(0)
        }

        onItemsChangedListener.forEach { it.invoke(requireNotNull(cart)) }
    }

    override fun onQuantityChanged(list: ShoppingCart?, item: ShoppingCart.Item?) {
        if (item?.product != null && list?.get(0) == item) {
            recyclerView.scrollToPosition(0)
        }

        update()
    }

    override fun onCleared(list: ShoppingCart?) {
        update()
        onItemsChangedListener.forEach { it.invoke(requireNotNull(cart)) }
    }

    override fun onItemRemoved(list: ShoppingCart?, item: ShoppingCart.Item?, pos: Int) {
        update()
        onItemsChangedListener.forEach { it.invoke(requireNotNull(cart)) }
    }

    override fun onProductsUpdated(list: ShoppingCart?) = update()
    override fun onPricesUpdated(list: ShoppingCart?) = update()
    override fun onCheckoutLimitReached(list: ShoppingCart?) = update()
    override fun onOnlinePaymentLimitReached(list: ShoppingCart?) = update()
    override fun onTaxationChanged(list: ShoppingCart?, taxation: ShoppingCart.Taxation?) {}
    override fun onViolationDetected(violations: MutableList<ViolationNotification>) {}
    override fun onCartDataChanged(list: ShoppingCart?) = update()
}