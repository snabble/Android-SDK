package io.snabble.sdk.wlanmanager.usecase.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

class ScanIsFinishedImpl(
    private val context: Context,
) : ScanIsFinished {

    override suspend fun invoke(): Boolean =
        withTimeoutOrNull(5.seconds) {
            suspendCancellableCoroutine { continuation ->

                val wifiScanReceiver = object : BroadcastReceiver() {

                    override fun onReceive(context: Context, intent: Intent) {
                        context.unregisterReceiver(this)
                        continuation.resume(true)
                    }
                }

                val intentFilter = IntentFilter()
                intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                context.registerReceiver(wifiScanReceiver, intentFilter)

                continuation.invokeOnCancellation {
                    context.unregisterReceiver(wifiScanReceiver)
                }
            }
        } ?: false
}
