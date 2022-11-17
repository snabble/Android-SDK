package io.snabble.sdk.widgets.snabble.wlan.usecases

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import io.snabble.sdk.Snabble
import io.snabble.sdk.wlanmanager.WlanManager

internal interface HasWlanConnectionUseCase {

    suspend operator fun invoke(ssid: String): Boolean
}

internal class HasWlanConnectionUseCaseImpl(
    private val snabble: Snabble,
    private val context: Context,
    private val wifiManager: WifiManager,
    private val connectivityManager: ConnectivityManager,
    private val wlanManager: WlanManager,
) : HasWlanConnectionUseCase {

    override suspend operator fun invoke(ssid: String): Boolean =
        snabble.currentCheckedInShop.value != null &&
                !suggestionAlreadySafed(ssid) &&
                wifiManager.isWifiEnabled &&
                !isConnectedToWifi() &&
                wlanManager.isWifiAvailable(ssid)

    @Suppress("DEPRECATION")
    private fun isConnectedToWifi(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            network != null
                    && connectivityManager.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }

    private fun suggestionAlreadySafed(ssid: String): Boolean {

        val sharedPreferences = context.getSharedPreferences("Suggestions", Context.MODE_PRIVATE)
        val suggestedSsid = sharedPreferences.getString("suggestion", "")
        return suggestedSsid == ssid
    }
}
