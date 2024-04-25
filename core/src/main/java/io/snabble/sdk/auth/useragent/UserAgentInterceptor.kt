package io.snabble.sdk.auth.useragent

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class UserAgentInterceptor(private val context: Context) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()
        val userAgentRequestBuilder = originalRequest.newBuilder()
        userAgentRequestBuilder.addHeader("User-Agent", context.getUserAgentHeader())

        context.getHeaderFields().forEach { headerField ->
            userAgentRequestBuilder.addHeader(headerField.key, headerField.value)
        }

        val finalRequest = userAgentRequestBuilder.build()

        return chain.proceed(finalRequest)
    }
}
