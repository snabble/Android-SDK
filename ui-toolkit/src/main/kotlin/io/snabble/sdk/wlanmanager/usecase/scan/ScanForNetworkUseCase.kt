package io.snabble.sdk.wlanmanager.usecase.scan

import io.snabble.sdk.data.Result

internal interface ScanForNetworkUseCase {

    operator fun invoke(): Result
}
