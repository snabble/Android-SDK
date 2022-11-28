package io.snabble.sdk.wlanmanager.usecase.scan

import android.net.wifi.WifiManager
import io.snabble.sdk.data.Error
import io.snabble.sdk.data.Result
import io.snabble.sdk.data.Success

/**
 * Up to Android 8.1 (Api 27) no restrictions were given
 */
internal class ScanForNetworkUseCaseLegacy(
    private val wifiManager: WifiManager,
) : ScanForNetworkUseCase {

    override fun invoke(): Result {
        @Suppress("DEPRECATION")
        val success = wifiManager.startScan()
        return if (success) {
            Success(message = "Scan started")
        } else {
            Error(message = "Scan failed")
        }
    }
}
