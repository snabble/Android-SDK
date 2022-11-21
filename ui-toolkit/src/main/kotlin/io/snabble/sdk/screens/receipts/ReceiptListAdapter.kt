package io.snabble.sdk.screens.receipts

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.snabble.sdk.ReceiptInfo
import io.snabble.sdk.ui.toolkit.R
import kotlinx.coroutines.CoroutineScope

internal class ReceiptListAdapter(private val lifecycleScope: CoroutineScope) :
    RecyclerView.Adapter<RecyclerView.ViewHolder?>() {

    var receiptInfoList: List<ReceiptInfo>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        ReceiptViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_receipt, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        receiptInfoList
            ?.get(pos)
            ?.let {
                (holder as? ReceiptViewHolder)?.bindTo(it, lifecycleScope)
            }
    }

    override fun getItemCount(): Int = receiptInfoList?.size ?: 0
}

private class ReceiptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val receiptItemHelper = ReceiptItemHelper(context = itemView.context, itemView = itemView)

    fun bindTo(receiptInfo: ReceiptInfo, lifecycleScope: CoroutineScope) {
        receiptItemHelper.bindTo(receiptInfo, false, lifecycleScope)
    }
}
