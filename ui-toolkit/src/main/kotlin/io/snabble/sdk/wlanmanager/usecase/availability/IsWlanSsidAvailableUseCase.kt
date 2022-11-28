package io.snabble.sdk.wlanmanager.usecase.availability

import io.snabble.sdk.data.Result

internal interface IsWlanSsidAvailableUseCase {

    operator fun invoke(ssid: String): Result
}
