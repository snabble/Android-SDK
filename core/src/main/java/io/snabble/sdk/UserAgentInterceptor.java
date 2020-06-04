package io.snabble.sdk;

import android.content.Context;
import android.os.Build;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttp;
import okhttp3.Request;
import okhttp3.Response;

class UserAgentInterceptor implements Interceptor {
    private String userAgent;

    public UserAgentInterceptor(Context context) {
        userAgent = context.getPackageManager().getApplicationLabel(context.getApplicationInfo())
                + "/" + Snabble.getInstance().getVersionName()
                + " snabble/" + Snabble.getVersion() +
                " (Android " + Build.VERSION.RELEASE
                + "; " + Build.BRAND + "; " + Build.MODEL + ")" +
                " okhttp " + OkHttp.VERSION;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", userAgent)
                .build();
        return chain.proceed(requestWithUserAgent);
    }
}
