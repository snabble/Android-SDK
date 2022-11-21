package io.snabble.sdk.screens.receipts

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.snabble.sdk.screens.receipts.usecase.GetReceiptsInfoUseCase
import io.snabble.sdk.screens.receipts.usecase.GetReceiptsInfoUseCaseImpl
import io.snabble.sdk.screens.receipts.usecase.GetReceiptsUseCase
import io.snabble.sdk.screens.receipts.usecase.GetReceiptsUseCaseImpl
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.utils.xx
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resumeWithException

class ReceiptProvider(
    private val context: Context,
    private val getReceiptsInfo: GetReceiptsInfoUseCase = GetReceiptsInfoUseCaseImpl(),
    private val getReceipts: GetReceiptsUseCase = GetReceiptsUseCaseImpl(),
) {

    private lateinit var progressDialog: ProgressDialog

    suspend fun showReceipt(receiptId: String) {
        coroutineScope {
            val progressDialogJob = launch {
                showProgressIndicator()
            }
            delay(5_000)
            val info = getReceiptsInfo(receiptId)
            val receipt = info?.let { getReceipts(it) }
            progressDialogJob.cancel()
            if (receipt != null) {
                show(receipt)
            }
        }
    }

    fun show(pdf: File) {
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
            //Todo: Handle Error
        }
    }

    private suspend fun showProgressIndicator() {
        suspendCancellableCoroutine<Unit> { continuation ->
            val progressDialog = ProgressDialog(context).apply {
                setProgressStyle(ProgressDialog.STYLE_SPINNER)
                setMessage(context.getString(R.string.Snabble_pleaseWait))
                setCancelable(true)
                setOnCancelListener {
                    // TODO: Reminder: receipts.cancelDownload()
                    if (!continuation.isCancelled) {
                        "Resuming with CancellationException".xx()
                        continuation.resumeWithException(CancellationException("Cancelled progress dialog"))
                    }
                }
            }
            progressDialog.show()

            continuation.invokeOnCancellation {
                "Job has been cancelled".xx()
                progressDialog.cancel()
            }
        }
    }
}
