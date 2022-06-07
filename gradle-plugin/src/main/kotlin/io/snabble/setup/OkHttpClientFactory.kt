package io.snabble.setup

import okhttp3.OkHttpClient
import okhttp3.CertificatePinner
import java.util.concurrent.TimeUnit

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

    internal fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder().apply {
        retryOnConnectionFailure(true)
        pingInterval(5, TimeUnit.SECONDS) // workaround for https://github.com/square/okhttp/issues/3146
        addInterceptor(UserAgentInterceptor())
        certificatePinner(CertificatePinner.Builder().apply {
            PINS.forEach { pin ->
                Environment.values().forEach { env ->
                    add(env.wildcardUrl, pin)
                }
            }
        }.build())
    }.build()
}