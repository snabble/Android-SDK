package io.snabble.sdk.wlanmanager.usecase.ssidcheck

import io.snabble.sdk.wlanmanager.data.Result

interface CheckSsid {

    operator fun invoke(ssid: String): Result
}
