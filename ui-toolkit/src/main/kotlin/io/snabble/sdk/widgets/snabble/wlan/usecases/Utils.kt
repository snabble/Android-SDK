package io.snabble.sdk.widgets.snabble.wlan.usecases

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun Context.isAnyPermissionGranted(): Boolean {
    return (permissionChangeWifiStateIsGranted() ||
            permissionCoarseLocationIsGranted() ||
            permissionFineLocationIsGranted())
}

fun Context.scanResultPermissionsGranted(): Boolean =
    permissionFineLocationIsGranted() && permissionChangeWifiStateIsGranted()

fun Context.permissionChangeWifiStateIsGranted() =
    ContextCompat.checkSelfPermission(this,
        Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED

fun Context.permissionCoarseLocationIsGranted() =
    ContextCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun Context.permissionFineLocationIsGranted() =
    ContextCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
