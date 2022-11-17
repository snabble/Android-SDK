package io.snabble.sdk.wlanmanager.usecase.ssidcheck

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACCESS_WIFI_STATE
import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import io.snabble.sdk.widgets.snabble.wlan.usecases.areAllGranted
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success

class CheckSsidApi29(
    private val context: Context,
    private val wifiManager: WifiManager,
) : CheckSsid {

    override fun invoke(ssid: String): Result {
        @SuppressLint("MissingPermission")
        if (hasPermissionForScanResults()) {
            val ssidAvailable =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    wifiManager.scanResults.any {
                        @Suppress("DEPRECATION")
                        it.SSID == ssid
                    }
                } else {
                    wifiManager.scanResults.any { it.wifiSsid.toString() == ssid }
                }
            return if (ssidAvailable) {
                Success(message = "Requested Wifi: $ssid is available")
            } else {
                Error(message = "Requested Wifi: $ssid is not available")
            }
        }
        return Error(message = "Missing permissions: ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE")
    }

    private fun hasPermissionForScanResults(): Boolean =
        context.areAllGranted(ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE)
}
