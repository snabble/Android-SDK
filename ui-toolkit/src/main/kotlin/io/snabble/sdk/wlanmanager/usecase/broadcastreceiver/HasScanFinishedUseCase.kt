package io.snabble.sdk.wlanmanager.usecase.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

internal interface HasScanFinished {

    suspend operator fun invoke(): Boolean
}

internal class ScanIsFinishedImpl(
    private val context: Context,
) : HasScanFinished {

    override suspend fun invoke(): Boolean =
        withTimeoutOrNull(5.seconds) {
            suspendCancellableCoroutine { continuation ->

                val scanFinishedReceiver = object : BroadcastReceiver() {

                    override fun onReceive(context: Context, intent: Intent) {
                        context.unregisterReceiver(this)
                        continuation.resume(true)
                    }
                }

                val intentFilter = IntentFilter()
                intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                ContextCompat.registerReceiver(
                    context,
                    scanFinishedReceiver,
                    intentFilter,
                    ContextCompat.RECEIVER_EXPORTED
                )

                continuation.invokeOnCancellation {
                    context.unregisterReceiver(scanFinishedReceiver)
                }
            }
        } ?: false
}
