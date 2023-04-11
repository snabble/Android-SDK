package io.snabble.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import okhttp3.Interceptor
import okhttp3.OkHttp
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class UserAgentInterceptor(context: Context) : Interceptor {

    private val userAgent: String

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(requestWithUserAgent)
    }

    init {
        val appName = (context.packageManager.getApplicationLabel(context.applicationInfo)).toString()
        val appVersion = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }
        } catch (e: Exception) {
            "Unknown"
        }
        userAgent = "$appName/$appVersion snabble/${Snabble.version} " +
                "(Android ${Build.VERSION.RELEASE}; ${Build.BRAND}; " +
                "${Build.MODEL}) okhttp/${OkHttp.VERSION}"
    }
}
