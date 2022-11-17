package io.snabble.sdk.wlanmanager.usecase.connectNetwork

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.annotation.RequiresApi
import io.snabble.sdk.wlanmanager.data.Error
import io.snabble.sdk.wlanmanager.data.Result
import io.snabble.sdk.wlanmanager.data.Success

class ConnectToWifiApi29(
    private val wifiManager: WifiManager,
    private val context: Context,
) : ConnectToWifi {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun invoke(ssid: String): Result {

        val sug = wifiNetworkSuggestion(ssid)

        val status = wifiManager.addNetworkSuggestions(listOf(sug))

        return if (status == STATUS_NETWORK_SUGGESTIONS_SUCCESS ||
            status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE
        ) {
            saveSuggestion(ssid)
            Success("Suggestion added")
        } else
            Error("Suggestion failed")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun wifiNetworkSuggestion(ssid: String) =
        WifiNetworkSuggestion
            .Builder()
            .setSsid(ssid)
            .setIsAppInteractionRequired(true)
            .setIsMetered(false)
            .build()

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveSuggestion(ssid: String) {
        val sharedPreferences = context.getSharedPreferences("Suggestions", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("suggestion", ssid).apply()
    }
}
