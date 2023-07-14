package io.snabble.sdk.ui.cart.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ShoppingCart.Item
import io.snabble.sdk.Snabble
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
        submitList(buildRows(context, cart), hasAnyImages)
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

        fun buildRows(context: Context, cart: ShoppingCart?): List<Row> = cart
            ?.mapNotNull { item -> item.asRow(context) }
            ?.let { rows ->
                if (cart.totalDepositPrice > 0) {
                    val priceFormatter = Snabble.instance.checkedInProject.getValue()?.priceFormatter
                    rows.toMutableList()
                        .apply {
                            add(
                                SimpleRow(
                                    text = priceFormatter?.format(cart.totalDepositPrice),
                                    title = context.getString(R.string.Snabble_Shoppingcart_deposit),
                                    imageResId = R.drawable.snabble_ic_deposit
                                )
                            )
                        }
                } else {
                    rows
                }
            }
            ?: emptyList()
    }
}

private fun Item.asRow(context: Context): Row? = when (type) {
    ShoppingCart.ItemType.LINE_ITEM -> {
        SimpleRow(
            item = this,
            text = if (isDiscount) {
                priceText.sanitize()
            } else if (isGiveaway) {
                context.getString(R.string.Snabble_Shoppingcart_giveaway)
            } else {
                null
            },
            title = if (isDiscount) {
                context.getString(R.string.Snabble_Shoppingcart_discounts)
            } else if (isGiveaway) {
                displayName
            } else {
                null
            },
            imageResId = if (isDiscount) {
                R.drawable.snabble_ic_percent
            } else if (isGiveaway) {
                R.drawable.snabble_ic_gift
            } else {
                null
            }
        )
    }

    ShoppingCart.ItemType.COUPON -> {
        SimpleRow(
            item = this,
            text = displayName,
            title = context.getString(R.string.Snabble_Shoppingcart_coupon),
            isDismissible = true
        )
    }

    ShoppingCart.ItemType.PRODUCT -> {
        ProductRow(
            item = this,
            isDismissible = true,
            name = displayName.sanitize(),
            subtitle = product?.subtitle?.sanitize(),
            imageUrl = product?.imageUrl?.sanitize(),
            encodingUnit = unit,
            priceText = totalPriceText?.sanitize() ?: "",
            quantityText = quantityText?.sanitize() ?: "",
            quantity = quantity,
            editable = isEditable,
            manualDiscountApplied = isManualCouponApplied
        )
    }

    else -> null
}

private fun String.sanitize(): String? = ifEmpty { null }
