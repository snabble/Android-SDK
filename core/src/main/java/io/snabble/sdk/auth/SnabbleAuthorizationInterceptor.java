package io.snabble.sdk.auth;

import java.io.IOException;

import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.utils.Logger;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class SnabbleAuthorizationInterceptor implements Interceptor {
    private Project project;
    private TokenRegistry tokenRegistry;

    public SnabbleAuthorizationInterceptor(Project project) {
        this.project = project;
        this.tokenRegistry = Snabble.getInstance().getTokenRegistry();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        String url = request.url().toString();

        if (url.startsWith(Snabble.getInstance().getEndpointBaseUrl())) {
            Token token = tokenRegistry.getToken(project);

            if (token != null) {
                request = request.newBuilder()
                        .addHeader("Client-Token", token.token)
                        .addHeader("Client-ID", Snabble.getInstance().getClientId())
                        .build();
            } else {
                Logger.e("No token for: " + project);
            }
        }

        return chain.proceed(request);
    }
}
