package io.snabble.sdk.wlanmanager.usecase.networkscan

import io.snabble.sdk.wlanmanager.data.Result

interface ScanForNetwork {

    operator fun invoke(): Result
}
