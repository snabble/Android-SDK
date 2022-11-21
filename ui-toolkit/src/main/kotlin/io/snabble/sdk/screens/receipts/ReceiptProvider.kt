package io.snabble.sdk.screens.receipts

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.core.content.FileProvider
import io.snabble.sdk.ReceiptInfo
import io.snabble.sdk.screens.receipts.usecase.GetReceiptsInfoUseCase
import io.snabble.sdk.screens.receipts.usecase.GetReceiptsInfoUseCaseImpl
import io.snabble.sdk.screens.receipts.usecase.GetReceiptsUseCase
import io.snabble.sdk.screens.receipts.usecase.GetReceiptsUseCaseImpl
import io.snabble.sdk.ui.toolkit.BuildConfig
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.utils.SnackbarUtils
import io.snabble.sdk.ui.utils.UIUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resumeWithException

class ReceiptProvider(
    private val context: Context,
    private val getReceiptsInfo: GetReceiptsInfoUseCase = GetReceiptsInfoUseCaseImpl(),
    private val getReceipts: GetReceiptsUseCase = GetReceiptsUseCaseImpl(),
) {

    suspend fun showReceipt(view: View, receiptId: String) {
        showReceipt(view) {
            getReceiptsInfo(receiptId)
        }
    }

    suspend fun showReceipt(view: View, info: ReceiptInfo) {
        showReceipt(view) { info }
    }

    private suspend fun showReceipt(view: View, receiptInfo: suspend () -> ReceiptInfo?) {
        val showReceiptJob = Job(parent = coroutineContext.job)
        val scope = CoroutineScope(Dispatchers.Main + showReceiptJob)
        scope.launch {
            val dialogJob = launch {
                try {
                    showProgressIndicator()
                } catch (e: CancelledDialogException) {
                    showReceiptJob.cancel()
                }
            }

            if (BuildConfig.DEBUG) delay(1_000)
            val receipt: File? = try {
                receiptInfo()
                    ?.let { getReceipts(it) }
            } catch (e: DownloadFailedException) {
                showSnackBar(view, R.string.Snabble_Receipt_errorDownload)
                null
            } finally {
                dialogJob.cancel()
            }
            if (receipt != null) {
                try {
                    show(receipt)
                } catch (e: MissingPdfReaderException) {
                    showSnackBar(view, R.string.Snabble_Receipt_pdfReaderUnavailable)
                }
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
            throw MissingPdfReaderException()
        }
    }

    private suspend fun showProgressIndicator() {
        suspendCancellableCoroutine<Unit> { continuation ->
            val dialog = AlertDialog.Builder(context)
                .setView(R.layout.circular_progress_indicator)
                .setCancelable(true)
                .setOnCancelListener {
                    if (!continuation.isCancelled) {
                        continuation.resumeWithException(CancelledDialogException())
                    }
                }
                .show()

            continuation.invokeOnCancellation {
                dialog.cancel()
            }
        }
    }
}

internal class CancelledDialogException(
    override val message: String = "Cancelled progress dialog",
) : Exception()

internal class MissingPdfReaderException(
    override val message: String = "PDF Reader unavailable",
) : Exception()

internal class DownloadFailedException(
    override val message: String = "Downloading Receipt failed",
) : Exception()

private fun showSnackBar(view: View, id: Int) =
    SnackbarUtils.make(view, id, UIUtils.SNACKBAR_LENGTH_VERY_LONG).show()
