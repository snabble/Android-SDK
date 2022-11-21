package io.snabble.sdk.screens.receipts.domain

import io.snabble.sdk.ReceiptInfo
import io.snabble.sdk.Receipts
import io.snabble.sdk.Snabble
import io.snabble.sdk.screens.receipts.DownloadFailedException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface ReceiptsInfoRepository {

    suspend fun getReceiptsInfo(receiptId: String): ReceiptInfo?
}

class ReceiptsInfoRepositoryImpl(
    private val snabble: Snabble = Snabble,
) : ReceiptsInfoRepository {

    override suspend fun getReceiptsInfo(receiptId: String): ReceiptInfo? =
        suspendCancellableCoroutine { continuation ->
            snabble.receipts.getReceiptInfo(object : Receipts.ReceiptInfoCallback {

                override fun success(receiptInfos: Array<ReceiptInfo>?) {
                    receiptInfos
                        ?.find { it.id == receiptId }
                        ?.let { receiptInfo ->
                            continuation.resume(receiptInfo)
                        }
                }

                override fun failure() {
                    continuation.resumeWithException(DownloadFailedException())
                }
            })
            continuation.invokeOnCancellation {
                continuation.resume(null)
            }
        }
}
