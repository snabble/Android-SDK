package io.snabble.sdk.wlanmanager.usecase.networkscan

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success

/**
 * Up to Android 8.1 (Api 27) no restrictions were given
 */
class ScanForNetworkLegacy(
    private val context: Context,
    private val wifiManager: WifiManager,
) : ScanForNetwork {

    override fun invoke(): Result {
        Log.d("xx", "invoke: legacy")
        @Suppress("DEPRECATION")
        val success = wifiManager.startScan()
        return if (success) {
            Success(message = "Scan started")
        } else {
            Error(message = "Scan failed")
        }
    }
}
