package io.snabble.sdk.ui.cart.shoppingcart.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.GestureHandler.DismissibleAdapter
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.UndoHelper
import io.snabble.sdk.ui.cart.shoppingcart.ShoppingCartView.Companion.buildRows
import io.snabble.sdk.ui.cart.shoppingcart.row.new.ProductRow
import io.snabble.sdk.ui.cart.shoppingcart.row.new.Row
import io.snabble.sdk.ui.cart.shoppingcart.row.new.SimpleRow
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.SnackbarUtils.make
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.utils.Logger

class ShoppingCartAdapter(private val parentView: View?, private val cart: ShoppingCart?) :
    RecyclerView.Adapter<LineItemViewHolder>(), UndoHelper, DismissibleAdapter {

    private var list = emptyList<Row>()
    private val context: Context? = parentView?.context
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
    fun fetchFrom(cart: ShoppingCart) {
        hasAnyImages = cart.any { !it?.product?.imageUrl.isNullOrEmpty() }
        context?.let {
            submitList(buildRows(it.resources, cart), hasAnyImages)
        }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineItemViewHolder {
        return LineItemViewHolder(ComposeView(parent.context))
//            if (viewType == TYPE_SIMPLE) {
//        val v = inflate(context, R.layout.snabble_item_shoppingcart_simple, null)
//                v.layoutParams = ViewGroup.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT
//                )
//                return SimpleViewHolder(v)
//            } else {
//                val v = inflate(context, R.layout.snabble_item_shoppingcart_product, null)
//                v.layoutParams = ViewGroup.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT
//                )
//                return ShoppingCartItemViewHolder(v, this)
//            }
    }

    override fun onBindViewHolder(holder: LineItemViewHolder, position: Int) {
        val type = getItemViewType(position)

        if (type == TYPE_PRODUCT) {
            holder.bind((getItem(position) as ProductRow), hasAnyImages)
        } else {
            holder.bind((getItem(position) as SimpleRow), hasAnyImages)
        }
    }

    override fun removeAndShowUndoSnackbar(adapterPosition: Int, item: ShoppingCart.Item) {
        if (adapterPosition == -1) {
            Logger.d("Invalid adapter position, ignoring")
            return
        }
        val cart = cart ?: return
        cart.remove(adapterPosition)
        Telemetry.event(Telemetry.Event.DeletedFromCart, item.product)
        parentView?.let {

            val snackbar = make(
                parentView,
                R.string.Snabble_Shoppingcart_articleRemoved, UIUtils.SNACKBAR_LENGTH_VERY_LONG
            )
            snackbar.setAction(R.string.Snabble_undo) { v: View? ->
                cart.insert(item, adapterPosition)
                fetchFrom(cart)
                Telemetry.event(Telemetry.Event.UndoDeleteFromCart, item.product)
            }
            snackbar.show()
        }
        fetchFrom(cart)
    }

    companion object {

        const val TYPE_PRODUCT = 0
        const val TYPE_SIMPLE = 1
    }
}
