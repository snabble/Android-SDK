package io.snabble.sdk.wlanmanager.usecase.connectNetwork

import io.snabble.sdk.wlanmanager.data.Result

interface ConnectToWifi {

    operator fun invoke(ssid: String): Result
}
