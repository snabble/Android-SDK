package io.snabble.sdk.screens.receipts

import android.content.Context
import android.view.View
import android.widget.TextView
import io.snabble.sdk.ReceiptInfo
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.widgets.snabble.purchase.RelativeTimeStringFormatter
import io.snabble.sdk.widgets.snabble.purchase.RelativeTimeStringFormatterImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.days

internal class ReceiptItemHelper(
    private val context: Context,
    private val relativeDateFormatter: RelativeTimeStringFormatter = RelativeTimeStringFormatterImpl(),
    private val itemView: View,
) {

    private val titleView: TextView = itemView.findViewById(R.id.shop_name)
    private val priceView: TextView = itemView.findViewById(R.id.price)
    private val dateView: TextView = itemView.findViewById(R.id.date)

    fun bindTo(receiptInfo: ReceiptInfo, newLineTimestamp: Boolean, lifecycleScope: CoroutineScope) {
        titleView.text = receiptInfo.shopName

        receiptInfo.pdfUrl ?: return

        priceView.text = receiptInfo.price
        dateView.text = formatDate(Date(receiptInfo.timestamp), newLineTimestamp)
        itemView.setOnClickListener {
            lifecycleScope.launch {
                showDetails(receiptInfo)
            }
        }
    }

    private suspend fun showDetails(receiptInfo: ReceiptInfo) {
        val receiptProvider = ReceiptProvider(context)
        receiptProvider.showReceipt(itemView, receiptInfo)
    }

    private fun formatDate(past: Date, newLineTimestamp: Boolean): String {
        val nowInMillis = Date().time
        val isYoungerThan1Day = nowInMillis - past.time < 1.days.inWholeMilliseconds
        return when {
            isYoungerThan1Day -> relativeDateFormatter.format(past.time, nowInMillis)

            newLineTimestamp -> DATE_FORMAT_NEW_LINE.format(past)

            else -> "${DATE_FORMAT.format(past)} ${context.getString(R.string.Snabble_Receipts_oClock)}"
        }
    }

    companion object {

        private val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy – HH:mm", Locale.ROOT)
        private val DATE_FORMAT_NEW_LINE = SimpleDateFormat("dd.MM.yyyy\nHH:mm", Locale.ROOT)
    }
}

suspend fun showDetails(view: View, receiptId: String) {
    val receiptProvider = ReceiptProvider(view.context)
    receiptProvider.showReceipt(view, receiptId)
}
