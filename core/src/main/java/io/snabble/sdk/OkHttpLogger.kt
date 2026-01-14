package io.snabble.sdk

import androidx.annotation.RestrictTo
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class OkHttpLogger @JvmOverloads constructor(
    private val logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT
) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val requestBody = request.body

        val connection = chain.connection()
        var requestStartMessage = "--> ${request.method} ${request.url}"
        if (connection != null) {
            requestStartMessage += " " + connection.protocol()
        }
        if (requestBody != null) {
            requestStartMessage += " (${requestBody.contentLength()}-byte body)"
        }
        if (request.headers.size > 0) {
            requestStartMessage += " with headers: " + request.headers.joinToString { it.first }
        }
        logger.log(requestStartMessage)

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            logger.log("<-- HTTP FAILED: $e")
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body
        val contentLength = responseBody.contentLength()
        var bodySize = if (contentLength != -1L) "$contentLength-byte" else "unknown-length"
        if (response.networkResponse == null) {
            bodySize = "cache-hit, $bodySize"
        } else if (response.cacheResponse != null) {
            bodySize = "cache-miss, $bodySize"
        }
        logger.log("<-- ${response.code}${if (response.message.isEmpty()) "" else ' ' + response.message} ${response.request.url} (${tookMs}ms, $bodySize body)")

        return response
    }
}
