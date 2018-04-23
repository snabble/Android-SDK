package io.snabble.sdk;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class SnabbleAuthorizationInterceptor implements Interceptor {
    private SnabbleSdk sdk;
    private String clientToken;

    public SnabbleAuthorizationInterceptor(SnabbleSdk sdk, String clientToken) {
        this.sdk = sdk;
        this.clientToken = clientToken;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        String url = request.url().toString();

        if (url.startsWith(sdk.getEndpointBaseUrl())) {
            request = request.newBuilder()
                    .addHeader("Client-Token", clientToken)
                    .addHeader("Client-ID", sdk.getClientId())
                    .build();
        }

        return chain.proceed(request);
    }
}
