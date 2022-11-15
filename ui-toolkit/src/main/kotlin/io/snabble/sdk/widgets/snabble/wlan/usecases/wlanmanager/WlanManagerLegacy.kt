package io.snabble.sdk.widgets.snabble.wlan.usecases.wlanmanager

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import io.snabble.sdk.widgets.snabble.wlan.usecases.Error
import io.snabble.sdk.widgets.snabble.wlan.usecases.Result
import io.snabble.sdk.widgets.snabble.wlan.usecases.Success
import io.snabble.sdk.widgets.snabble.wlan.usecases.scanResultPermissionsGranted

/**
 * WifiManger for Api 28 and below.
 */
@Suppress("DEPRECATION")
class WlanManagerLegacyImpl(
    private val context: Context,
    private val wifiManager: WifiManager,
) : WlanManager {

    override fun isWifiAvailable(ssid: String): Result {
        scanForResults()
        return isSsidAvailable(ssid)
    }

    override fun connectToWifi(ssid: String): Result {
        val wifiConf = WifiConfiguration()
        wifiConf.SSID = "\"$ssid\""
        wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)

        val netId = wifiManager.addNetwork(wifiConf)
        return startWifiConnectionForNetwork(netId)
    }

    /**
     * Deprecation can be suppressed since it's only deprecated in Api 29
     */
    private fun startWifiConnectionForNetwork(netId: Int): Result {
        wifiManager.disconnect()
        val netEnabled = wifiManager.enableNetwork(netId, true)
        if (!netEnabled) {
            return Error(message = "Suggested Network ID couldn't be activated")
        }
        val isConnectionSuccessful = wifiManager.reconnect()
        return if (isConnectionSuccessful) {
            Success(message = "Connection success")
        } else {
            Error(message = "Connection failed")
        }
    }

    /**
     * Deprecation can be suppressed since it's only deprecated in Api 33
     */
    @SuppressLint("MissingPermission")
    private fun isSsidAvailable(ssid: String): Result {
        if (context.scanResultPermissionsGranted()) {
            return if (wifiManager.scanResults.any { it.SSID == ssid }) {
                Success(message = "Requested Wifi: $ssid is available")
            } else {
                Error(message = "Requested Wifi: $ssid is not available")
            }
        }
        return Error(message = "Missing permissions: ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE")
    }

    /**
     * WifiManager.startScan() is deprecated since Api 28 and will be removed in future.
     * Since their is no alternative it is allowed to use is with some restrictions following:
     * https://developer.android.com/guide/topics/connectivity/wifi-scan#wifi-scan-restrictions
     *
     * A successful call to ScanResults requires both of the following permissions:
     *
     *  ACCESS_FINE_LOCATION
     *  ACCESS_WIFI_STATE
     *
     * Since these two permissions are needed to access the scan result and at the same time
     * cover all restrictions in the docs above, it is fine to call the scan method in Api lvl
     * if both permissions are granted.
     */
    @SuppressLint("MissingPermission")
    private fun scanForResults() {
        if (context.scanResultPermissionsGranted()) {
            wifiManager.startScan()
        }
    }
}
