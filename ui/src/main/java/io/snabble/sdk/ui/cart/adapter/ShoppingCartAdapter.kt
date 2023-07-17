package io.snabble.sdk.ui.cart.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ShoppingCart.Item
import io.snabble.sdk.ui.GestureHandler.DismissibleAdapter
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.UndoHelper
import io.snabble.sdk.ui.cart.adapter.viewholder.ShoppingCartItemViewHolder
import io.snabble.sdk.ui.cart.adapter.viewholder.SimpleViewHolder
import io.snabble.sdk.ui.cart.adapter.viewholder.SnabbleSimpleRow
import io.snabble.sdk.ui.cart.adapter.viewholder.depositItem
import io.snabble.sdk.ui.cart.adapter.viewholder.simpleRowFromCoupon
import io.snabble.sdk.ui.cart.adapter.viewholder.simpleRowFromLineItem
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.SnackbarUtils
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.inflate
import io.snabble.sdk.utils.Logger

class ShoppingCartAdapter(
    private val parentView: View,
    private val cart: ShoppingCart?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    UndoHelper,
    DismissibleAdapter {

    private var list = emptyList<Row>()
    private val context: Context = parentView.context
    private var hasAnyImages = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        TYPE_SIMPLE -> SimpleViewHolder(itemView = parent.inflate(R.layout.snabble_item_shoppingcart_simple))

        TYPE_PRODUCT -> ShoppingCartItemViewHolder(
            itemView = parent.inflate(R.layout.snabble_item_shoppingcart_product),
            undoHelper = this
        )

        else -> throw IllegalArgumentException("Missing ViewHolder for viewType <$viewType>.")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_SIMPLE -> {
                val viewHolder = holder as SimpleViewHolder
                viewHolder.update(getItem(position) as SnabbleSimpleRow, hasAnyImages)
            }

            TYPE_PRODUCT -> {
                val viewHolder = holder as ShoppingCartItemViewHolder
                viewHolder.bindTo((getItem(position) as ProductRow), hasAnyImages)
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is SnabbleSimpleRow) {
            TYPE_SIMPLE
        } else {
            TYPE_PRODUCT
        }

    override fun getItemCount(): Int = list.size

    override fun isDismissible(position: Int): Boolean = getItem(position).isDismissible

    override fun removeAndShowUndoSnackbar(adapterPosition: Int, item: Item?) {
        val cart = cart ?: return
        if (adapterPosition == -1) {
            Logger.d("Invalid adapter position, ignoring")
            return
        }

        cart.remove(adapterPosition)
        Telemetry.event(Telemetry.Event.DeletedFromCart, item?.product ?: "null_item")

        SnackbarUtils.make(
            parentView = parentView,
            resId = R.string.Snabble_Shoppingcart_articleRemoved,
            duration = UIUtils.SNACKBAR_LENGTH_VERY_LONG
        )
            .apply {
                setAction(R.string.Snabble_undo) { _ ->
                    cart.insert(item, adapterPosition)
                    cart.let(::fetchFrom)
                    Telemetry.event(Telemetry.Event.UndoDeleteFromCart, item?.product ?: "null_item")
                }
            }
            .show()

        fetchFrom(cart)
    }

    // for fetching the data from outside of this view
    fun fetchFrom(cart: ShoppingCart) {
        submitList(
            newList = buildRows(context, cart),
            hasAnyImages = cart
                .any { it.type == ShoppingCart.ItemType.PRODUCT && it.product?.imageUrl?.isNotBlank() == true }
        )
    }

    fun submitList(newList: List<Row>, hasAnyImages: Boolean) {
        this.hasAnyImages = hasAnyImages

        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            override fun getOldListSize(): Int = list.size

            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldRow = list[oldItemPosition]
                val newRow = newList[newItemPosition]
                return if (oldRow.item == null || newRow.item == null) {
                    false
                } else {
                    oldRow.item === newRow.item
                }
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                list[oldItemPosition] == newList[newItemPosition]
        })
        list = newList
        diffResult.dispatchUpdatesTo(this)
    }

    fun getItem(position: Int): Row = list[position]

    companion object {

        private const val TYPE_PRODUCT = 0
        private const val TYPE_SIMPLE = 1

        fun buildRows(context: Context, cart: ShoppingCart): List<Row> = cart
            .map { item: Item -> item.asRow(context) }
            .toMutableList()
            .apply { add(cart.depositItem(context)) }
            .filterNotNull()
    }
}

private fun Item.asRow(context: Context): Row? = when (type) {
    ShoppingCart.ItemType.LINE_ITEM -> simpleRowFromLineItem(context)

    ShoppingCart.ItemType.COUPON -> simpleRowFromCoupon(context)

    ShoppingCart.ItemType.PRODUCT -> productRowFromProduct()

    else -> null
}
