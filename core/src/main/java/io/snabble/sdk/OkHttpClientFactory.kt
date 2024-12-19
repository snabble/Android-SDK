package io.snabble.sdk

import android.app.Application
import androidx.annotation.RestrictTo
import io.snabble.sdk.auth.useragent.UserAgentInterceptor
import io.snabble.sdk.utils.LetsEncryptCertHelper
import io.snabble.sdk.utils.Logger
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object OkHttpClientFactory {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun createOkHttpClient(application: Application): OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(application.cacheDir, 10 * 1024 * 1024))
        .retryOnConnectionFailure(true)
        .pingInterval(5, TimeUnit.SECONDS) // workaround for https://github.com/square/okhttp/issues/3146
        .addInterceptor(OkHttpLogger { message: String? -> Logger.i(message) })
        .addInterceptor(UserAgentInterceptor(application))
        .apply { Snabble.config.networkInterceptor?.let { addNetworkInterceptor(it) } }
        .apply { LetsEncryptCertHelper.addLetsEncryptCertificatesForMarshmallowOrEarlier(this) }
        .build()
}
