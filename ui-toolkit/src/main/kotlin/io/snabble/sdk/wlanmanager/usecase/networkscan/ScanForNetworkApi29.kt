package io.snabble.sdk.wlanmanager.usecase.networkscan

import android.Manifest.permission.*
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import io.snabble.sdk.widgets.snabble.wlan.usecases.areAllGranted
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success

class ScanForNetworkApi29(
    private val context: Context,
    private val wifiManager: WifiManager,
) : ScanForNetwork {

    override fun invoke(): Result {
        Log.d("xx", "invoke: 29")
        return if (hasPermissionForScanning()) {
            @Suppress("DEPRECATION")
            val success = wifiManager.startScan()
            if (success) {
                Success(message = "Scan started")
            } else {
                Error(message = "Scan failed")
            }
        } else {
            Error(message = "Missing Permission: ACCESS_FINE_LOCATION, CHANGE_WIFI_STATE")
        }
    }

    private fun hasPermissionForScanning(): Boolean =
        context.areAllGranted(ACCESS_FINE_LOCATION, CHANGE_WIFI_STATE)
}
