package io.snabble.sdk

import android.os.Build
import android.os.LocaleList
import androidx.annotation.RestrictTo
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class AcceptedLanguageInterceptor : Interceptor {
    private val formatter = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))

    private val acceptedLanguagesHeader: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val list = LocaleList.getDefault()
            val all = (0 until list.size()).map { i -> list.get(i) }
            val simplified = (all.map { it.toLanguageTag() } + all.map { it.language }).distinct()
            var str = simplified.first()
            for (i in 1 until simplified.size) {
                val weight = formatter.format(1 - (1 / simplified.size.toFloat()) * i)
                str += ",${simplified[i]};q=$weight"
            }
            str
        } else Locale.getDefault().language

    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        val url = request.url.toString()
        val baseUrl = Snabble.endpointBaseUrl
        if (url.startsWith(baseUrl)) {
            request = request.newBuilder()
                .addHeader("Accept-Language", acceptedLanguagesHeader)
                .build()
        }
        return chain.proceed(request)
    }
}