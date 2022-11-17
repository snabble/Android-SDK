package io.snabble.sdk.wlanmanager.usecase.connectNetwork

import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.annotation.RequiresApi
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success

class ConnectToWifiApi30(
    private val wifiManager: WifiManager,
) : ConnectToWifi {

    @RequiresApi(Build.VERSION_CODES.R)
    override fun invoke(ssid: String): Result {
        val sug = wifiNetworkSuggestion(ssid)

        val status = wifiManager.addNetworkSuggestions(listOf(sug))

        return if (status == STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Success("Suggestion added")
        } else
            Error("Suggestion failed")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun wifiNetworkSuggestion(ssid: String) =
        WifiNetworkSuggestion
            .Builder()
            .setSsid(ssid)
            .setIsInitialAutojoinEnabled(true)
            .setIsAppInteractionRequired(true)
            .setIsMetered(false)
            .build()
}
