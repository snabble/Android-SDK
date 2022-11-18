package io.snabble.sdk.widgets.snabble.wlan.usecases

import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import io.snabble.sdk.Snabble
import io.snabble.sdk.wlanmanager.WlanManager
import io.snabble.sdk.wlanmanager.WlanManagerImpl.Companion.KEY_SUGGESTIONS

internal interface HasWlanConnectionUseCase {

    suspend operator fun invoke(ssid: String): Boolean
}

internal class HasWlanConnectionUseCaseImpl(
    private val snabble: Snabble,
    private val wifiManager: WifiManager,
    private val connectivityManager: ConnectivityManager,
    private val wlanManager: WlanManager,
    private val sharedPrefs: SharedPreferences,
) : HasWlanConnectionUseCase {

    /**
     * Wifi should only be shown under the following conditions:
     * - The user is checked in into a shop
     * - Wifi is enabled
     * - no Suggestion has been saved
     * - is not connected a wifi
     * - the wanted wifi is available
     */
    override suspend operator fun invoke(ssid: String): Boolean =
        snabble.currentCheckedInShop.value != null &&
                wifiManager.isWifiEnabled &&
                !suggestionAlreadySaved(ssid) &&
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

    private fun suggestionAlreadySaved(ssid: String): Boolean =
        sharedPrefs.getString(KEY_SUGGESTIONS, "") == ssid
}
