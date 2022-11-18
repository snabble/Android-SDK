package io.snabble.sdk.wlanmanager.utils

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.content.ContextCompat

internal fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED

internal fun Context.isAnyGranted(vararg permission: String): Boolean = permission.any(::isGranted)

internal fun Context.areAllGranted(vararg permission: String): Boolean = permission.all(::isGranted)
