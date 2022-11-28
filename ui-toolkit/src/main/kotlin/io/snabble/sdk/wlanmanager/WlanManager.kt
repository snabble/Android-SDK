package io.snabble.sdk.wlanmanager

import io.snabble.sdk.data.Error
import io.snabble.sdk.data.Result
import io.snabble.sdk.data.Success
import io.snabble.sdk.wlanmanager.usecase.broadcastreceiver.HasScanFinished
import io.snabble.sdk.wlanmanager.usecase.connect.ConnectToWlanUseCase
import io.snabble.sdk.wlanmanager.usecase.scan.ScanForNetworkUseCase
import io.snabble.sdk.wlanmanager.usecase.availability.IsWlanSsidAvailableUseCase

internal interface WlanManager {

    suspend fun isWlanAvailable(ssid: String): Boolean

    fun connectToWlan(ssid: String): Result
}

/**
 * WifiManager for every Api.
 *
 * Follows the following restrictions:
 * https://developer.android.com/guide/topics/connectivity/wifi-scan#wifi-scan-restrictions
 *
 * Picks matching use case depending on the Api lvl
 */

internal class WlanManagerImpl(
    private val hasScanFinished: HasScanFinished,
    private val scanForNetwork: ScanForNetworkUseCase,
    private val isWlanSsidAvailable: IsWlanSsidAvailableUseCase,
    private val connectToWlanUseCase: ConnectToWlanUseCase,
) : WlanManager {

    override suspend fun isWlanAvailable(ssid: String): Boolean {
        scanForNetwork()
        val isScanFinished = hasScanFinished()
        return if (isScanFinished) {
            when (isWlanSsidAvailable(ssid)) {
                is Error -> false
                is Success -> true
            }
        } else {
            false
        }
    }

    override fun connectToWlan(ssid: String): Result {
        return connectToWlanUseCase(ssid)
    }

    companion object {

        const val PREFS_WLAN = "WlanPrefs"
        const val KEY_SUGGESTIONS = "Suggestions"
    }
}
