package io.snabble.sdk;

import android.app.Application;
import io.snabble.sdk.auth.SnabbleAuthorizationInterceptor;
import io.snabble.sdk.utils.Logger;
import okhttp3.Cache;
import okhttp3.CertificatePinner;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

class OkHttpClientFactory {
    private static final String LETSENCRYPT_X3 = "sha256/YLh1dUR9y6Kja30RrAn7JKnbQG/uEtLMkBgFF2Fuihg=";
    private static final String LETSENCRYPT_X4 = "sha256/sRHdihwgkaib1P1gxX8HFszlD+7/gTfNvuAybgLPNis=";

    static OkHttpClient createOkHttpClient(Application application,
                                           Project project) {
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

        if (project != null) {
            builder.addInterceptor(new SnabbleAuthorizationInterceptor(project));
        }

        builder.addInterceptor(new UserAgentInterceptor(application));

        builder.certificatePinner(new CertificatePinner.Builder()
                .add("*.snabble.io", LETSENCRYPT_X3)
                .add("*.snabble-staging.io", LETSENCRYPT_X3)
                .add("*.snabble-testing.io", LETSENCRYPT_X3)
                .add("*.snabble.io", LETSENCRYPT_X4)
                .add("*.snabble-staging.io", LETSENCRYPT_X4)
                .add("*.snabble-testing.io", LETSENCRYPT_X4)
                .build());

        if (config.sslSocketFactory != null && config.x509TrustManager != null) {
            builder.sslSocketFactory(config.sslSocketFactory, config.x509TrustManager);
        }

        return builder.build();
    }
}
