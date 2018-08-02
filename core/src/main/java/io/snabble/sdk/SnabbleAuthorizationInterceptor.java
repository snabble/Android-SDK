package io.snabble.sdk;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class SnabbleAuthorizationInterceptor implements Interceptor {
    private String clientToken;

    public SnabbleAuthorizationInterceptor(String clientToken) {
        this.clientToken = clientToken;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        String url = request.url().toString();

        if (url.startsWith(Snabble.getInstance().getEndpointBaseUrl())) {
            request = request.newBuilder()
                    .addHeader("Client-Token", clientToken)
                    .addHeader("Client-ID", Snabble.getInstance().getClientId())
                    .build();
        }

        return chain.proceed(request);
    }
}
