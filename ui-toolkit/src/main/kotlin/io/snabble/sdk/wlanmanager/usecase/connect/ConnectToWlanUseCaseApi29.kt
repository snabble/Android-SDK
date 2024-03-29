package io.snabble.sdk.wlanmanager.usecase.connect

import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.annotation.RequiresApi
import io.snabble.sdk.wlanmanager.WlanManagerImpl.Companion.KEY_SUGGESTIONS
import io.snabble.sdk.data.Error
import io.snabble.sdk.data.Result
import io.snabble.sdk.data.Success

internal class ConnectToWlanUseCaseApi29(
    private val sharedPrefs: SharedPreferences,
    private val wifiManager: WifiManager,
) : ConnectToWlanUseCase {

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

    private fun saveSuggestion(ssid: String) {
        sharedPrefs.edit().putString(KEY_SUGGESTIONS, ssid).apply()
    }
}
