package io.snabble.sdk.screens.receipts

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.core.content.FileProvider
import io.snabble.sdk.ReceiptInfo
import io.snabble.sdk.Receipts.ReceiptDownloadCallback
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.utils.SnackbarUtils
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.widgets.snabble.purchase.RelativeTimeStringFormatter
import io.snabble.sdk.widgets.snabble.purchase.RelativeTimeStringFormatterImpl
import org.apache.commons.io.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

internal class ReceiptItemHelper(
    private val context: Context,
    private val relativeDateFormatter: RelativeTimeStringFormatter = RelativeTimeStringFormatterImpl(),
    private val itemView: View,
) {

    private val titleView: TextView = itemView.findViewById(R.id.shop_name)
    private val priceView: TextView = itemView.findViewById(R.id.price)
    private val dateView: TextView = itemView.findViewById(R.id.date)

    fun bindTo(receiptInfo: ReceiptInfo, newLineTimestamp: Boolean) {
        titleView.text = receiptInfo.shopName

        receiptInfo.pdfUrl ?: return

        priceView.text = receiptInfo.price
        dateView.text = formatDate(Date(receiptInfo.timestamp), newLineTimestamp)
        itemView.setOnClickListener {
            val receipts = Snabble.receipts
            val progressDialog = ProgressDialog(context).apply {
                setProgressStyle(ProgressDialog.STYLE_SPINNER)
                setMessage(context.getString(R.string.Snabble_pleaseWait))
                setCancelable(true)
                setOnCancelListener {
                    receipts.cancelDownload()
                }
            }
            progressDialog.show()

            receipts.download(
                receiptInfo,
                object : ReceiptDownloadCallback {
                    override fun success(pdf: File?) {
                        pdf ?: return

                        if (FileUtils.sizeOf(pdf) == 0L) {
                            pdf.delete()
                            receipts.download(receiptInfo, this)
                            return
                        }

                        progressDialog.cancel()

                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.ReceiptFileProvider",
                                pdf
                            )
                            setDataAndType(uri, "application/pdf")
                        }
                        val activity: Activity? = UIUtils.getHostActivity(context)
                        try {
                            activity?.startActivity(intent)
                        } catch (ignored: Exception) {
                            if (!itemView.isAttachedToWindow) return

                            try {
                                SnackbarUtils
                                    .make(
                                        itemView,
                                        R.string.Snabble_Receipt_pdfReaderUnavailable,
                                        UIUtils.SNACKBAR_LENGTH_VERY_LONG
                                    )
                                    .show()
                            } catch (ignored: IllegalArgumentException) {
                                // no suitable parent view found
                            }
                        }
                    }

                    override fun failure() {
                        progressDialog.cancel()

                        if (!itemView.isAttachedToWindow) return

                        SnackbarUtils
                            .make(
                                itemView,
                                R.string.Snabble_Receipt_errorDownload,
                                UIUtils.SNACKBAR_LENGTH_VERY_LONG
                            )
                            .show()
                    }
                })
        }
    }

    private fun formatDate(past: Date, newLineTimestamp: Boolean): String {
        val nowInMillis = Date().time
        val isYoungerThan1Day = nowInMillis - past.time < TimeUnit.DAYS.toMillis(1)
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
