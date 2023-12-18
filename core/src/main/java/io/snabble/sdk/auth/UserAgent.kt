package io.snabble.sdk.auth

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import io.snabble.sdk.extensions.getApplicationInfoCompat
import io.snabble.sdk.extensions.getPackageInfoCompat
import io.snabble.sdk.extensions.xx

fun getUserAgentHeaders(context: Context) {
    context.getAppLabel().xx("appname:")
    context.getMajorVersion().xx("significant version:")
    context.getBuildVersion().xx("buildnumber:")
    context.getVersionName().xx("versionNumber:")
    "Android".xx()
    getAndroidVersion().xx("OS version:")
    getHardwareType().xx("Hardware:")


}

fun getAndroidVersion(): Int = Build.VERSION.SDK_INT

fun getHardwareType() = Build.MODEL

fun Context.getAppLabel(): String = try {
    packageManager.getApplicationInfoCompat(applicationInfo.packageName, 0)
    packageManager.getApplicationLabel(applicationInfo).toString()
} catch (e: NameNotFoundException) {
    "Unknown"
}

fun Context.getBuildVersion(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    packageManager.getPackageInfoCompat(applicationInfo.packageName, 0)?.longVersionCode.toString()
} else {
    @Suppress("DEPRECATION")
    packageManager.getPackageInfoCompat(applicationInfo.packageName, 0)?.versionCode.toString()
}

fun Context.getVersionName() = packageManager.getPackageInfoCompat(applicationInfo.packageName, 0)?.versionName

fun Context.getMajorVersion() = getVersionName()?.split(".")?.first()
