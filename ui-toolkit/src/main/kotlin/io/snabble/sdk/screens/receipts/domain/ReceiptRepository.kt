package io.snabble.sdk.screens.receipts.domain

import io.snabble.sdk.ReceiptInfo
import io.snabble.sdk.Receipts
import io.snabble.sdk.Snabble
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.coroutines.resume

interface ReceiptsRepository {

    suspend fun getReceipts(receiptInfo: ReceiptInfo): File?
}

class ReceiptsRepositoryImpl(
    private val snabble: Snabble = Snabble,
) : ReceiptsRepository {

    override suspend fun getReceipts(receiptInfo: ReceiptInfo): File? =
        suspendCancellableCoroutine { continuation ->
            snabble.receipts.download(
                receiptInfo,
                object : Receipts.ReceiptDownloadCallback {
                    override fun success(pdf: File?) {
                        if (pdf == null) {
                            continuation.resume(null)
                        }

                        if (FileUtils.sizeOf(pdf) == 0L) {
                            pdf?.delete()
                            snabble.receipts.download(receiptInfo, this)
                            return
                        }

                        continuation.resume(pdf)
                    }

                    override fun failure() {
                        continuation.resume(null)
                    }
                })

            continuation.invokeOnCancellation {
                continuation.resume(null)
            }
        }
}
