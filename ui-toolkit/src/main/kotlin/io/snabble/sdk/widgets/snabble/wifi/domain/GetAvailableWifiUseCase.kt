package io.snabble.sdk.widgets.snabble.wifi.domain

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import io.snabble.sdk.Snabble

internal class GetAvailableWifiUseCase(
    private val wifiManager: WifiManager,
    private val connectivityManager: ConnectivityManager,
) {

    operator fun invoke(): Boolean =
        Snabble.currentCheckedInShop.value != null
            && wifiManager.isWifiEnabled
            && !isConnectedToStoreWifi()


    @Suppress("DEPRECATION")
    private fun isConnectedToStoreWifi(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            network != null && connectivityManager.getNetworkCapabilities(network)?.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) == true
        } else {
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }
}
