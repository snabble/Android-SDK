package io.snabble.sdk.ui.widgets.purchase

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.ReceiptInfo
import io.snabble.sdk.Receipts
import io.snabble.sdk.Snabble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

internal class PurchaseViewModel : ViewModel() {

    var state: State by mutableStateOf(Loading)
        private set

    fun updatePurchases() {
        viewModelScope.launch {
            val purchases = loadPurchases()
            state = ShowPurchases(purchases)
        }
    }

    private suspend fun loadPurchases(): List<Purchase> = withContext(Dispatchers.IO) {
        Log.d("Hello", "Launched update!")
        suspendCancellableCoroutine { continuation ->
            Snabble.receipts.getReceiptInfo(object : Receipts.ReceiptInfoCallback {

                override fun success(receiptInfos: Array<ReceiptInfo>?) {
                    if (continuation.isActive) {
                        if (receiptInfos != null) {
                            val orders = receiptInfos.toList()
                            val purchases = orders
                                .slice(0 until orders.size.coerceAtMost(2))
                                .map { it.toPurchase() }
                            continuation.resume(purchases)
                        } else {
                            continuation.resume(emptyList())
                        }
                    }
                }

                override fun failure() {
                    if (continuation.isActive) {
                        continuation.cancel()
                    }
                }
            })
        }
    }
}

internal sealed class State
internal object Loading : State()
internal data class ShowPurchases(val data: List<Purchase>) : State()
