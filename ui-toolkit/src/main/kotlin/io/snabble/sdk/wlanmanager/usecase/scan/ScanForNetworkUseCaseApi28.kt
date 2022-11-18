package io.snabble.sdk.wlanmanager.usecase.scan

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CHANGE_WIFI_STATE
import android.content.Context
import android.net.wifi.WifiManager
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success
import io.snabble.sdk.wlanmanager.utils.isAnyGranted
import io.snabble.sdk.wlanmanager.utils.isGranted

internal class ScanForNetworkUseCaseApi28(
    private val context: Context,
    private val wifiManager: WifiManager,
) : ScanForNetworkUseCase {

    override fun invoke(): Result {
        return if (hasPermissionForScanning()) {
            @Suppress("DEPRECATION")
            val success = wifiManager.startScan()
            if (success) {
                Success(message = "Scan started")
            } else {
                Error(message = "Scan failed")
            }
        } else {
            Error(message = "Missing Permission: ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, CHANGE_WIFI_STATE")
        }
    }

    private fun hasPermissionForScanning(): Boolean =
        context.isAnyGranted(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION) && context.isGranted(CHANGE_WIFI_STATE)
}
