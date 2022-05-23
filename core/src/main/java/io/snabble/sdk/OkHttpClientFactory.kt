package io.snabble.sdk

import android.app.Application
import androidx.annotation.RestrictTo
import okhttp3.OkHttpClient
import io.snabble.sdk.OkHttpLogger
import io.snabble.sdk.Snabble
import io.snabble.sdk.UserAgentInterceptor
import io.snabble.sdk.OkHttpClientFactory
import io.snabble.sdk.utils.LetsEncryptCertHelper
import io.snabble.sdk.utils.Logger
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object OkHttpClientFactory {
    private val PINS = arrayOf(
        "sha256/YLh1dUR9y6Kja30RrAn7JKnbQG/uEtLMkBgFF2Fuihg=",  // Let's Encrypt X3 cross-signed
        "sha256/sRHdihwgkaib1P1gxX8HFszlD+7/gTfNvuAybgLPNis=",  // Let's Encrypt X4 cross-signed
        "sha256/J2/oqMTsdhFWW/n85tys6b4yDBtb6idZayIEBx7QTxA=",  // Let's Encrypt E1
        "sha256/vZNucrIS7293MQLGt304+UKXMi78JTlrwyeUIuDIknA=",  // Let's Encrypt E2
        "sha256/jQJTbIh0grw0/1TkHSumWb+Fs0Ggogr621gT3PvPKG0=",  // Let's Encrypt R3 cross-signed
        "sha256/5VReIRNHJBiRxVSgOTTN6bdJZkpZ0m1hX+WPd5kPLQM=",  // Let's Encrypt R4 cross-signed
        // backup CAs
        "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=",  // ISRG Root X1
        "sha256/lCppFqbkrlJ3EcVFAkeip0+44VaoJUymbnOaEUk7tEU=",  // AddTrust External Root
        "sha256/r/mIkG3eEpVdm+u/ko/cwxzOMo1bk4TyHIlByibiA5E=",  // DigiCert Global Root
        "sha256/i7WTqTvh0OioIruIfFR4kMPnBqrS2rdiVPl/s2uC/CY=",  // DigiCert Global Root G2
        "sha256/WoiWRyIOVNa9ihaBciRSC7XHjliYS9VwUGOIud4PB18=",  // DigiCert HA Root
        "sha256/h6801m+z8v3zbgkRHpq6L29Esgfzhj89C1SyUCOQmqU=",  // GeoTrust Global
        "sha256/q5hJUnat8eyv8o81xTBIeB5cFxjaucjmelBPT2pRMo8=",  // GeoTrust PCA G3 Root
        "sha256/47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=",  // GeoTrust PCA G4
        "sha256/SQVGZiOrQXi+kqxcvWWE96HhfydlLVqFr4lQTqI5qqo=" // GeoTrust PCA
    )

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun createOkHttpClient(application: Application): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.cache(Cache(application.cacheDir, 10 * 1024 * 1024))
        builder.retryOnConnectionFailure(true)
        builder.pingInterval(5, TimeUnit.SECONDS) // workaround for https://github.com/square/okhttp/issues/3146
        builder.addInterceptor(OkHttpLogger(HttpLoggingInterceptor.Logger { message: String? ->
            Logger.i(message)
        }))
        Snabble.config.networkInterceptor?.let {
            builder.addNetworkInterceptor(it)
        }
        builder.addInterceptor(UserAgentInterceptor(application))
        if (!Snabble.config.disableCertificatePinning) {
            val environments = Environment.values()
            builder.certificatePinner(CertificatePinner.Builder().apply {
                PINS.forEach { pin ->
                    environments.forEach { env ->
                        add(env.wildcardUrl, pin)
                    }
                }
            }.build())
        }
        LetsEncryptCertHelper.addLetsEncryptCertificatesForMarshmallowOrEarlier(builder)
        return builder.build()
    }
}