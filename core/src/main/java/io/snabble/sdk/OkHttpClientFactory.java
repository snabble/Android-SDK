package io.snabble.sdk;

import android.app.Application;
import java.util.concurrent.TimeUnit;
import io.snabble.sdk.utils.LetsEncryptCertHelper;
import io.snabble.sdk.utils.Logger;
import okhttp3.Cache;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

class OkHttpClientFactory {
    private static final String[] PINS = new String[] {
            "sha256/YLh1dUR9y6Kja30RrAn7JKnbQG/uEtLMkBgFF2Fuihg=", // Let's Encrypt X3 cross-signed
            "sha256/sRHdihwgkaib1P1gxX8HFszlD+7/gTfNvuAybgLPNis=", // Let's Encrypt X4 cross-signed
            "sha256/J2/oqMTsdhFWW/n85tys6b4yDBtb6idZayIEBx7QTxA=", // Let's Encrypt E1
            "sha256/vZNucrIS7293MQLGt304+UKXMi78JTlrwyeUIuDIknA=", // Let's Encrypt E2
            "sha256/jQJTbIh0grw0/1TkHSumWb+Fs0Ggogr621gT3PvPKG0=", // Let's Encrypt R3 cross-signed
            "sha256/5VReIRNHJBiRxVSgOTTN6bdJZkpZ0m1hX+WPd5kPLQM=", // Let's Encrypt R4 cross-signed

            // backup CAs
            "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=", // ISRG Root X1
            "sha256/lCppFqbkrlJ3EcVFAkeip0+44VaoJUymbnOaEUk7tEU=", // AddTrust External Root
            "sha256/r/mIkG3eEpVdm+u/ko/cwxzOMo1bk4TyHIlByibiA5E=", // DigiCert Global Root
            "sha256/i7WTqTvh0OioIruIfFR4kMPnBqrS2rdiVPl/s2uC/CY=", // DigiCert Global Root G2
            "sha256/WoiWRyIOVNa9ihaBciRSC7XHjliYS9VwUGOIud4PB18=", // DigiCert HA Root
            "sha256/h6801m+z8v3zbgkRHpq6L29Esgfzhj89C1SyUCOQmqU=", // GeoTrust Global
            "sha256/q5hJUnat8eyv8o81xTBIeB5cFxjaucjmelBPT2pRMo8=", // GeoTrust PCA G3 Root
            "sha256/47DEQpj8HBSa+/TImW+5JCeuQeRkm5NMpJWZG3hSuFU=", // GeoTrust PCA G4
            "sha256/SQVGZiOrQXi+kqxcvWWE96HhfydlLVqFr4lQTqI5qqo="  // GeoTrust PCA
    };

    static OkHttpClient createOkHttpClient(Application application) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.cache(new Cache(application.getCacheDir(), 10 * 1024 * 1024));
        builder.retryOnConnectionFailure(true);
        builder.pingInterval(5, TimeUnit.SECONDS); // workaround for https://github.com/square/okhttp/issues/3146

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(Logger::i);

        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        builder.addInterceptor(logging);

        Config config = Snabble.getInstance().getConfig();
        builder.addInterceptor(new UserAgentInterceptor(application));

        if (config.networkInterceptor != null) {
            builder.addNetworkInterceptor(config.networkInterceptor);
        }

        if (!Snabble.getInstance().getConfig().disableCertificatePinning) {
            Environment[] environments = Environment.values();
            CertificatePinner.Builder certificatePinnerBuilder = new CertificatePinner.Builder();
            for (String pin : PINS) {
                for (Environment env : environments) {
                    certificatePinnerBuilder.add(env.getWildcardUrl(), pin);
                }
            }
            builder.certificatePinner(certificatePinnerBuilder.build());
        }

        LetsEncryptCertHelper.addLetsEncryptCertificatesForMarshmallowOrEarlier(builder);

        return builder.build();
    }
}