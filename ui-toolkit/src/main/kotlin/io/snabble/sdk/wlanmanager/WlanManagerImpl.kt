package io.snabble.sdk.wlanmanager

import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success
import io.snabble.sdk.wlanmanager.usecase.broadcastreceiver.ScanIsFinished
import io.snabble.sdk.wlanmanager.usecase.connectNetwork.ConnectToWifi
import io.snabble.sdk.wlanmanager.usecase.networkscan.ScanForNetwork
import io.snabble.sdk.wlanmanager.usecase.ssidcheck.CheckSsid

/**
 * WifiManager for every Api.
 *
 * Follows the following restrictions:
 * https://developer.android.com/guide/topics/connectivity/wifi-scan#wifi-scan-restrictions
 *
 * Picks matching use case depending on the Api lvl
 */

class WlanManagerImpl(
    private val scanIsFinished: ScanIsFinished,
    private val scanForNetwork: ScanForNetwork,
    private val checkSsid: CheckSsid,
    private val connectToWifiUseCase: ConnectToWifi,
) : WlanManager {

    override suspend fun isWifiAvailable(ssid: String): Boolean {
        scanForNetwork()
        val resultsAvailable = scanIsFinished()

        return if (resultsAvailable) {
            when (checkSsid(ssid)) {
                is Error -> false
                is Success -> true
            }
        } else
            false
    }

    override fun connectToWifi(ssid: String): Result {
        return connectToWifiUseCase(ssid)
    }

    companion object {

        const val PREFS_WLAN = "WlanPrefs"
        const val KEY_SUGGESTIONS = "Suggestions"
    }
}
