package io.snabble.sdk.widgets.snabble.wlan.usecases

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.xx

internal interface HasWlanConnectionUseCase {

    operator fun invoke(): Boolean
}

internal class HasWlanConnectionUseCaseImpl(
    private val snabble: Snabble,
    private val wifiManager: WifiManager,
    private val connectivityManager: ConnectivityManager,
    private val isStoreWifiAvailable: IsStoreWifiAvailable,
) : HasWlanConnectionUseCase {

    override operator fun invoke(): Boolean =
        snabble.currentCheckedInShop.value != null
                && wifiManager.isWifiEnabled
                && !isConnectedToStoreWifi()

    @Suppress("DEPRECATION")
    private fun isConnectedToStoreWifi(): Boolean {
        isStoreWifiAvailable("AndroidWifi").xx("wifi is available")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            network != null
                    && connectivityManager.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }
}

interface IsStoreWifiAvailable {

    operator fun invoke(ssid: String): Boolean
}

class IsStoreWifiAvailableImpl(
    private val context: Context,
    private val wifiManager: WifiManager,
) : IsStoreWifiAvailable {

    override fun invoke(ssid: String): Boolean {
        var scanResults: List<ScanResult>? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scanResults = scanForResultsAboveApi29()
            scanResults.xx("")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            scanResults = scanForResultsAboveApi28()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scanResults = scanForResultsAboveApi23()
        }

        return isSsidAvailable(scanResults, ssid)
    }

    private fun isSsidAvailable(scanResults: List<ScanResult>?, ssid: String): Boolean {
        scanResults ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {

            scanResults.let { scanResultList ->
                return scanResultList.any { it.SSID == ssid }
            }
        } else {
            scanResults.let { scanResultList ->
                return scanResultList.any { it.wifiSsid.toString() == ssid }
            }
        }
    }

    private fun scanForResultsAboveApi29(): List<ScanResult>? {
        @SuppressLint("MissingPermission")
        if (context.permissionFineLocationIsGranted() && context.permissionChangeWifiStateIsGranted()) {
            //WifiNetworkSuggestions instead?
            wifiManager.startScan()
            Log.d("xx", "invoke 29+: ${wifiManager.scanResults}")
            return wifiManager.scanResults
        }
        return null
    }

    private fun scanForResultsAboveApi28(): List<ScanResult>? {
        @SuppressLint("MissingPermission")
        if ((context.permissionFineLocationIsGranted() || context.permissionCoarseLocationIsGranted()) && context.permissionChangeWifiStateIsGranted()) {
            wifiManager.startScan()
            Log.d("xx", "invoke 28+: ${wifiManager.scanResults}")
            return wifiManager.scanResults
        }
        return null
    }

    private fun scanForResultsAboveApi23(): List<ScanResult>? {
        @SuppressLint("MissingPermission")
        if (context.isAnyPermissionGranted()) {
            wifiManager.startScan()
            Log.d("xx", "invoke 23+: ${wifiManager.scanResults}")
            return wifiManager.scanResults
        }
        return null
    }

    private fun Context.isAnyPermissionGranted(): Boolean {
        return (permissionChangeWifiStateIsGranted() ||
                permissionCoarseLocationIsGranted() ||
                permissionFineLocationIsGranted())
    }

    private fun Context.permissionChangeWifiStateIsGranted() =
        ContextCompat.checkSelfPermission(this,
            Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED

    private fun Context.permissionCoarseLocationIsGranted() =
        ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun Context.permissionFineLocationIsGranted() =
        ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

interface ConnectToWlanUseCase {

    operator fun invoke(ssid: String): Result
}

class ConnectToWlanUseCaseImpl(
    private val wifiManager: WifiManager,
    private val connectivityManager: ConnectivityManager,
) : ConnectToWlanUseCase {

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    override fun invoke(ssid: String): Result {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {

            val wifiConf = WifiConfiguration()
            wifiConf.SSID = "\"" + ssid + "\""
            wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            var netId = wifiManager.addNetwork(wifiConf).xx()
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true).xx("enabled")
            val isConnectionSuccessful = wifiManager.reconnect()
            isConnectionSuccessful.xx("attempt to connect: ")
        } else {
            wifiManager.removeNetworkSuggestions(emptyList())
            val wifiSug =
                WifiNetworkSuggestion
                    .Builder().apply {
                        setSsid(ssid)
                        setPriority(40)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            setMacRandomizationSetting(WifiNetworkSuggestion.RANDOMIZATION_PERSISTENT)
                        }
                        setIsAppInteractionRequired(true)
                        setIsInitialAutojoinEnabled(true)
                        setIsMetered(false)
                    }
                    .build()
            var status = wifiManager.addNetworkSuggestions(listOf(wifiSug))
            val success = status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
            Log.d("xx", "invoke: $success")
        }

        return Success("connectet")
    }
}

sealed interface Result {

    val message: String
}

data class Success(
    override val message: String,
) : Result

data class Error(
    override val message: String,
) : Result
