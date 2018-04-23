package io.snabble.sdk;

import android.content.Context;
import android.os.Build;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class UserAgentInterceptor implements Interceptor {
    private String userAgent;

    public UserAgentInterceptor(Context context) {
        userAgent = context.getPackageManager().getApplicationLabel(context.getApplicationInfo())
                + "/" + BuildConfig.VERSION_NAME + " " +
                "(Android " + Build.VERSION.RELEASE
                + "; " + Build.BRAND + "; " + Build.MODEL + ")" +
                " okhttp/" + context.getString(R.string.okhttp_version);
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
