package io.snabble.sdk.widgets.snabble.purchase.repository

import io.snabble.sdk.ReceiptInfo
import io.snabble.sdk.Receipts
import io.snabble.sdk.Snabble
import io.snabble.sdk.widgets.snabble.purchase.Purchase
import io.snabble.sdk.widgets.snabble.purchase.RelativeTimeStringFormatter
import io.snabble.sdk.widgets.snabble.purchase.toPurchase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

internal interface PurchasesRepository {

    suspend fun getPurchases(count: Int): List<Purchase>
}

internal class PurchasesRepositoryImpl(
    private val Snabble: Snabble,
    private val timeFormatter: RelativeTimeStringFormatter,
) : PurchasesRepository {

    override suspend fun getPurchases(count: Int): List<Purchase> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            Snabble.receipts.getReceiptInfo(object : Receipts.ReceiptInfoCallback {

                override fun success(receiptInfos: Array<ReceiptInfo>?) {
                    if (!continuation.isActive) return

                    val purchases: List<Purchase> = receiptInfos.mapToPurchases(count)
                    continuation.resume(purchases)
                }

                override fun failure() {
                    if (continuation.isActive) continuation.cancel()
                }
            })
        }
    }

    private fun Array<ReceiptInfo>?.mapToPurchases(count: Int): List<Purchase> {
        val receiptInfoList = this?.toList() ?: emptyList()
        return receiptInfoList
            .slice(0 until receiptInfoList.size.coerceAtMost(count))
            .map { it.toPurchase(timeFormatter) }
    }
}
