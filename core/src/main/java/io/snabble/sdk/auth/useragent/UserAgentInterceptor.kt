package io.snabble.sdk.auth.useragent

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

internal class UserAgentInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder()
            .addHeader("User-Agent", context.getUserAgentHeader())
            .apply { context.getHeaderFields().forEach { (key, value) -> addHeader(key, value) } }
            .build()
        )
}
