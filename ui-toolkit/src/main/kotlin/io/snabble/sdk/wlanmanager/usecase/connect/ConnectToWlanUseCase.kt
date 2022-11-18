package io.snabble.sdk.wlanmanager.usecase.connect

import io.snabble.sdk.wlanmanager.data.Result

internal interface ConnectToWlanUseCase {

    operator fun invoke(ssid: String): Result
}
