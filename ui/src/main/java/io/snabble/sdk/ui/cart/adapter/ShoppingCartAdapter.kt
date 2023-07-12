package io.snabble.sdk.ui.cart.adapter

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.Snabble.instance
import io.snabble.sdk.ui.GestureHandler.DismissibleAdapter
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.UndoHelper
import io.snabble.sdk.ui.cart.adapter.viewholder.ShoppingCartItemViewHolder
import io.snabble.sdk.ui.cart.adapter.viewholder.SimpleRow
import io.snabble.sdk.ui.cart.adapter.viewholder.SimpleViewHolder
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.SnackbarUtils.make
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.inflate
import io.snabble.sdk.utils.Logger

class ShoppingCartAdapter(private val parentView: View?, private val cart: ShoppingCart?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), UndoHelper, DismissibleAdapter {

    private var list = emptyList<Row>()
    private val context: Context = parentView!!.context
    private var hasAnyImages = false

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is SimpleRow) {
            TYPE_SIMPLE
        } else TYPE_PRODUCT
    }

    override fun isDismissible(position: Int): Boolean {
        return getItem(position).isDismissible
    }

    override fun getItemCount(): Int {
        return list.size
    }

    // for fetching the data from outside of this view
    fun fetchFrom(cart: ShoppingCart?) {
        hasAnyImages = (0 until cart!!.size())
            .asSequence()
            .map { cart[it] }
            .filter { it.type == ShoppingCart.ItemType.PRODUCT }
            .map { it.product }
            .map { it!!.imageUrl }
            .any { !it.isNullOrEmpty() }
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
                return if (oldRow.item == null || newRow.item == null) {
                    false
                } else oldRow.item === newRow.item
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_SIMPLE) {
            SimpleViewHolder(
                itemView = parent.inflate(R.layout.snabble_item_shoppingcart_simple).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                })
        } else {
            ShoppingCartItemViewHolder(
                itemView = parent.inflate(R.layout.snabble_item_shoppingcart_product).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                },
                undoHelper = this
            )
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

    override fun removeAndShowUndoSnackbar(adapterPosition: Int, item: ShoppingCart.Item?) {
        if (adapterPosition == -1) {
            Logger.d("Invalid adapter position, ignoring")
            return
        }
        cart!!.remove(adapterPosition)
        Telemetry.event(Telemetry.Event.DeletedFromCart, item!!.product)
        val snackbar = make(
            parentView!!,
            R.string.Snabble_Shoppingcart_articleRemoved, UIUtils.SNACKBAR_LENGTH_VERY_LONG
        )
        snackbar.setAction(R.string.Snabble_undo) { _: View? ->
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

        private fun sanitize(input: String?): String? {
            return if (input != null && input == "") null else input
        }

        fun buildRows(resources: Resources, cart: ShoppingCart?): List<Row> {
            val rows: MutableList<Row> = ArrayList(
                cart!!.size() + 1
            )
            cart.forEach { item ->
                if (item.type == ShoppingCart.ItemType.LINE_ITEM) {
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
                } else if (item.type == ShoppingCart.ItemType.COUPON) {
                    val row = SimpleRow()
                    row.item = item
                    row.title = resources.getString(R.string.Snabble_Shoppingcart_coupon)
                    row.text = item.displayName
                    row.isDismissible = true
                    rows.add(row)
                } else if (item.type == ShoppingCart.ItemType.PRODUCT) {
                    val row = ProductRow()
                    val product = item.product
                    val quantity = item.quantity
                    if (product != null) {
                        row.subtitle = sanitize(product.subtitle)
                        row.imageUrl = sanitize(product.imageUrl)
                    }
                    row.name = sanitize(item.displayName)
                    row.encodingUnit = item.unit
                    row.priceText = item?.totalPriceText?.let(::sanitize) ?: ""
                    row.quantity = quantity
                    row.quantityText = item?.quantityText?.let(::sanitize) ?: ""
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
