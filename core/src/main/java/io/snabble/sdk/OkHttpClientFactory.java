package io.snabble.sdk;

import android.app.Application;
import io.snabble.sdk.auth.SnabbleAuthorizationInterceptor;
import io.snabble.sdk.utils.Logger;
import okhttp3.Cache;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

class OkHttpClientFactory {
    private static final String[] PINS = new String[] {
            "sha256/YLh1dUR9y6Kja30RrAn7JKnbQG/uEtLMkBgFF2Fuihg=", // Let's Encrypt X3
            "sha256/sRHdihwgkaib1P1gxX8HFszlD+7/gTfNvuAybgLPNis=", // Let's Encrypt X4
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

        builder.cache(new Cache(application.getCacheDir(), 10485760)); //10 MB

        builder.retryOnConnectionFailure(true);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        Logger.i(message);
                    }
                });

        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        builder.addInterceptor(logging);

        Snabble.Config config = Snabble.getInstance().getConfig();
        builder.addInterceptor(new UserAgentInterceptor(application));

        if (!Snabble.getInstance().getConfig().disableCertificatePinning) {
            CertificatePinner.Builder certificatePinnerBuilder = new CertificatePinner.Builder();
            for (String pin : PINS) {
                certificatePinnerBuilder.add("*.snabble.io", pin);
                certificatePinnerBuilder.add("*.snabble-staging.io", pin);
                certificatePinnerBuilder.add("*.snabble-testing.io", pin);
            }
            builder.certificatePinner(certificatePinnerBuilder.build());
        }

        if (config.sslSocketFactory != null && config.x509TrustManager != null) {
            builder.sslSocketFactory(config.sslSocketFactory, config.x509TrustManager);
        }

        return builder.build();
    }
}
