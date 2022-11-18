package io.snabble.sdk.wlanmanager.usecase.connect

import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success

internal class ConnectToWlanUseCaseLegacy(
    private val wifiManager: WifiManager,
) : ConnectToWlanUseCase {

    override fun invoke(ssid: String): Result {
        val wifiConf = createWlanConfiguration(ssid)

        @Suppress("DEPRECATION")
        val netId = wifiManager.addNetwork(wifiConf)

        return startWifiConnectionForNetwork(netId)
    }

    @Suppress("DEPRECATION")
    private fun createWlanConfiguration(ssid: String): WifiConfiguration =
        WifiConfiguration().apply {
            SSID = "\"$ssid\""
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
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
