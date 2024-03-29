package io.snabble.sdk.extensions

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.ResolveInfo
import android.os.Build

@Throws(NameNotFoundException::class)
fun PackageManager.getPackageInfoCompat(
    packageName: String,
    flags: Int = 0
): PackageInfo? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, flags)
    }

@Throws(NameNotFoundException::class)
fun PackageManager.getApplicationInfoCompat(
    packageName: String,
    flags: Int = PackageManager.GET_META_DATA
): ApplicationInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getApplicationInfo(packageName, flags)
    }

@Throws(UnsupportedOperationException::class)
fun PackageManager.getQueryIntentActivitiesCompat(
    intent: Intent,
    flags: Int = PackageManager.MATCH_DEFAULT_ONLY
): List<ResolveInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(flags.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        queryIntentActivities(intent, flags)
    }
