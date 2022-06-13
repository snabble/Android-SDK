package io.snabble.setup

import okhttp3.Interceptor
import okhttp3.OkHttp
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.*

internal class UserAgentInterceptor : Interceptor {
    private val userAgent: String

    init {
        val props = Properties()
        props.load(this::class.java.classLoader.getResourceAsStream("snabble.properties"))
        val version = props.getProperty("version")
        val osName = System.getProperty("os.name")
        val osArch = System.getProperty("os.arch")
        val osVersion = System.getProperty("os.version")

        userAgent = "SnabbleGradlePlugin/$version ($osName $osVersion; $osArch) okhttp/${OkHttp.VERSION}"
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}