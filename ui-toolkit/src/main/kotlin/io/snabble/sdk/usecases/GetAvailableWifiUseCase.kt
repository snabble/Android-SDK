package io.snabble.sdk.usecases

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.snabble.sdk.Snabble

internal class GetAvailableWifiUseCase(
    private val wifiManager: WifiManager,
    private val connectivityManager: ConnectivityManager,
) {

    operator fun invoke(): MutableState<Boolean> {
        val connectionAvailable: Boolean =
            Snabble.currentCheckedInShop.value != null && wifiManager.isWifiEnabled && !isConnectedToStoreWifi()
        return mutableStateOf(connectionAvailable)
    }

    // FIXME: Deal w/ deprecation warning
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
