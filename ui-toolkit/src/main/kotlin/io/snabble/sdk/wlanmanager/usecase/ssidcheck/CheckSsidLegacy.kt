package io.snabble.sdk.wlanmanager.usecase.ssidcheck

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CHANGE_WIFI_STATE
import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success
import io.snabble.sdk.wlanmanager.utils.isAnyGranted

class CheckSsidLegacy(
    private val context: Context,
    private val wifiManager: WifiManager,
) : CheckSsid {

    override fun invoke(ssid: String): Result {
        @SuppressLint("MissingPermission")
        if (hasPermissionForScanResults())
            return if (wifiManager.scanResults.any {
                    @Suppress("DEPRECATION")
                    it.SSID == ssid
                }) {
                Success(message = "Requested Wifi: $ssid is available")
            } else {
                Error(message = "Requested Wifi: $ssid is not available")
            }
        return Error(message = "Missing permissions: ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, CHANGE_WIFI_STATE")
    }

    private fun hasPermissionForScanResults(): Boolean =
        context.isAnyGranted(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, CHANGE_WIFI_STATE)
}
