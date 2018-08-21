package io.snabble.sdk;

import android.app.Application;

import io.snabble.sdk.auth.SnabbleAuthorizationInterceptor;
import io.snabble.sdk.utils.Logger;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

class OkHttpClientFactory {
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

        if(config.sslSocketFactory != null && config.x509TrustManager != null) {
            builder.sslSocketFactory(config.sslSocketFactory, config.x509TrustManager);
        }

        return builder.build();
    }
}
