package io.snabble.sdk.sample

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.call().request().url
        Log.d("LoggingInterceptor", url.toString())
        return chain.proceed(chain.request())
    }
}