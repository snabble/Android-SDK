package io.snabble.sdk

import android.os.Build
import android.os.LocaleList
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.*

class AcceptedLanguageInterceptor : Interceptor {
    private val acceptedLanguagesHeader: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val list = LocaleList.getDefault()
            val all = (0 until list.size()).map { i -> list.get(i) }
            val simplified = (all.map { it.toLanguageTag() } + all.map { it.language }).distinct()
            var str = simplified.first()
            for (i in 1 until simplified.size) {
                str += ",${simplified[i]};q=${
                    "%.1f".format(
                        Locale.US,
                        1 - (1 / simplified.size.toFloat()) * i
                    )
                }"
            }
            str
        } else Locale.getDefault().language

    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        val url = request.url.toString()
        val baseUrl = Snabble.getInstance().endpointBaseUrl
        if (baseUrl != null && url.startsWith(baseUrl)) {
            request = request.newBuilder()
                .addHeader("Accept-Language", acceptedLanguagesHeader)
                .build()
        }
        return chain.proceed(request)
    }
}