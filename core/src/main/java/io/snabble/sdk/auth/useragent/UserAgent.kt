package io.snabble.sdk.auth.useragent

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Build
import io.snabble.sdk.BuildConfig
import io.snabble.sdk.extensions.getPackageInfoCompat

// User agent header, example ->
// SnabbleSdkSample/1.0(1) Android/34 (Google; sdk_gphone64_arm64) SDK/dev
fun Context.getUserAgentHeader(): String {
    val appDescription = "${getAppName()}/${getVersionNumber()}(${getBuildVersion()})"
    val osDescription = "Android/$ANDROID_VERSION"

    return "$appDescription $osDescription ($HARDWARE_DESCRIPTOR) SDK/$SDK_VERSION"
}

/// HTTP headerFields using user agent keys defined in https://wicg.github.io/ua-client-hints/
///
/// `Sec-CH-UA: "SnabbleSdkSample";v="1"`
/// `Sec-CH-UA-Full-Version-List: "SnabbleSdkSample";v="1.0-1","SDK";v="dev"`
/// `Sec-CH-UA-Platform: Android`
/// `Sec-CH-UA-Platform-Version: 34`
/// `Sec-CH-UA-Arch: sdk_gphone64_arm64`
fun Context.getHeaderFields(): Map<String, String> = mapOf(
    "Sec-CH-UA" to "\"${getAppName()}\";v=\"${getMajorVersion()}\"",
    "Sec-CH-UA-Full-Version-List" to "\"${getAppName()}\";v=\"${getVersionNumber()}-${getBuildVersion()}\",\"SDK\";v=\"$SDK_VERSION\"",
    "Sec-CH-UA-Platform" to "Android",
    "Sec-CH-UA-Platform-Version" to ANDROID_VERSION,
    "Sec-CH-UA-Arch" to HARDWARE_DESCRIPTOR
)

private const val SDK_VERSION = BuildConfig.VERSION_NAME
private val ANDROID_VERSION = Build.VERSION.SDK_INT.toString()
private val HARDWARE_DESCRIPTOR = "${Build.MANUFACTURER}; ${Build.PRODUCT}"

private fun Context.getAppName(): String = packageManager.getApplicationLabel(applicationInfo).toString()

private fun Context.getBuildVersion(): String = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageManager.getPackageInfoCompat(applicationInfo.packageName)?.longVersionCode.toString()
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfoCompat(applicationInfo.packageName)?.versionCode.toString()
    }
} catch (e: NameNotFoundException) {
    "Unknown"
}

private fun Context.getVersionNumber() = try {
    packageManager.getPackageInfoCompat(applicationInfo.packageName)?.versionName
} catch (e: NameNotFoundException) {
    "Unknown"
}

private fun Context.getMajorVersion() = getVersionNumber()?.split(".")?.first()
