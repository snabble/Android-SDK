package io.snabble.sdk.wlanmanager.usecase.networkscan

import android.Manifest.permission.*
import android.content.Context
import android.net.wifi.WifiManager
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success
import io.snabble.sdk.wlanmanager.utils.areAllGranted

class ScanForNetworkApi29(
    private val context: Context,
    private val wifiManager: WifiManager,
) : ScanForNetwork {

    override fun invoke(): Result {
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
