package io.snabble.sdk.wlanmanager.usecase.connectNetwork

import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success

class ConnectToWifiLegacy(
    private val wifiManager: WifiManager,
) : ConnectToWifi {

    override fun invoke(ssid: String): Result {
        val wifiConf = createWifiConfiguration(ssid)

        @Suppress("DEPRECATION")
        val netId = wifiManager.addNetwork(wifiConf)

        return startWifiConnectionForNetwork(netId)
    }

    @Suppress("DEPRECATION")
    private fun createWifiConfiguration(ssid: String): WifiConfiguration {
        val wifiConf = WifiConfiguration()
        wifiConf.SSID = "\"$ssid\""
        wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        return wifiConf
    }

    @Suppress("DEPRECATION")
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
}
