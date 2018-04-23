package io.snabble.sdk;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class ExponentialRetryRequestInterceptor implements Interceptor {
    private int tryCount;

    public ExponentialRetryRequestInterceptor() {
        this(3);
    }

    public ExponentialRetryRequestInterceptor(int tryCount) {
        this.tryCount = tryCount;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        int currentTry = 0;
        int maxCount = tryCount - 1;
        while (currentTry < maxCount) {
            currentTry++;

            try {
                Response response = chain.proceed(request);
                int code = response.code();
                if (code < 500 && code != 408) {
                    return response;
                }
            } catch (IOException e) {
                // most likely timeout or transfer error, ignore and try again
            }

            try {
                long delayMs = (long) (Math.pow(2, currentTry) * 1000.0);
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {

            }
        }

        return chain.proceed(request);
    }
}
