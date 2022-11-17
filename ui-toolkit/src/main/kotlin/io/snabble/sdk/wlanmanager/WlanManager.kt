package io.snabble.sdk.wlanmanager

import io.snabble.sdk.wlanmanager.data.Result

interface WlanManager {

    suspend fun isWifiAvailable(ssid: String): Boolean
    fun connectToWifi(ssid: String): Result
}
