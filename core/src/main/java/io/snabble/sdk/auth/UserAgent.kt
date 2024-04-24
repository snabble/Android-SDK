package io.snabble.sdk.auth

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import io.snabble.sdk.BuildConfig
import io.snabble.sdk.extensions.getPackageInfoCompat
import io.snabble.sdk.extensions.xx

fun Context.getUserAgentHeader(): String {
    val appDescription = "${getAppName()}/${getVersionNumber()}(${getBuildVersion()})"
    val osDescription = "Android/${getAndroidVersion()}"

    return "$appDescription $osDescription (${getHardwareType()}) SDK/${SDK_VERSION}".xx()
}

fun Context.getHeaderFields(): Map<String, String> = mapOf(
    "Sec-CH-UA" to "\"${getAppName()}\";v=\"${getMajorVersion()}\"",
    "Sec-CH-UA-Full-Version-List" to "\"${getAppName()}\";v=\"${getVersionNumber()}-${getBuildVersion()}\",\"SDK\";v=\"${getSdkVersion()}\"",
    "Sec-CH-UA-Platform" to "Android",
    "Sec-CH-UA-Platform-Version" to getAndroidVersion(),
    "Sec-CH-UA-Arch" to getHardwareType()
)
private const val SDK_VERSION = BuildConfig.VERSION_NAME
private fun getSdkVersion() = BuildConfig.VERSION_NAME

private fun getAndroidVersion() = Build.VERSION.SDK_INT.toString()

private fun getHardwareType() = Build.MODEL

private fun Context.getAppName(): String = try {
    packageManager.getApplicationLabel(applicationInfo).toString()
} catch (e: NameNotFoundException) {
    "Unknown"
}

private fun Context.getBuildVersion(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    packageManager.getPackageInfoCompat(applicationInfo.packageName, 0)?.longVersionCode.toString()
} else {
    @Suppress("DEPRECATION")
    packageManager.getPackageInfoCompat(applicationInfo.packageName, 0)?.versionCode.toString()
}

private fun Context.getVersionNumber() = packageManager.getPackageInfoCompat(applicationInfo.packageName, 0)?.versionName

private fun Context.getMajorVersion() = getVersionNumber()?.split(".")?.first()
