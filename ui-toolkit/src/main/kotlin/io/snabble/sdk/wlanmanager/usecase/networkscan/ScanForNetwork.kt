package io.snabble.sdk.wlanmanager.usecase.networkscan

import io.snabble.sdk.wlanmanager.data.Result

/**
 * WifiManager.startScan() is deprecated since Api 28 and will be removed in future.
 * Since their is no alternative it is allowed to use is with some restrictions following:
 *
 * https://developer.android.com/guide/topics/connectivity/wifi-scan#wifi-scan-restrictions
 *
 */

interface ScanForNetwork {

    operator fun invoke(): Result
}
