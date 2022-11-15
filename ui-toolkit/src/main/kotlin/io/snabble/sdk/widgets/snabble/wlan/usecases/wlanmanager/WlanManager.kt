package io.snabble.sdk.widgets.snabble.wlan.usecases.wlanmanager

import io.snabble.sdk.widgets.snabble.wlan.usecases.Result

interface WlanManager {

    fun isWifiAvailable(ssid: String): Result
    fun connectToWifi(ssid: String): Result
}
