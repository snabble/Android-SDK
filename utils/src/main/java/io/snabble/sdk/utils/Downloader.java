package io.snabble.sdk.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class Downloader {
    private long maxCacheTime;
    private String url;
    private String requestUrl;
    private final Object lock = new Object();

    private int inMemoryCacheTime = 0;
    private Call call;

    private final OkHttpClient client;

    private int retryCount = 0;
    private int currentTry = 0;

    private boolean fromCache;

    private final Map<String, String> headers = new HashMap<>();

    private final Handler handler = new Handler(Looper.getMainLooper());

    public Downloader(OkHttpClient okHttpClient) {
        client = okHttpClient;
    }

    /**
     * Downloads the specified endpoint by setPath as a string and calls onResponse on success.
     */
    public void loadAsync() {
        loadAsync(null);
    }

    /**
     * Downloads the specified endpoint by setPath as a string and calls onResponse on success.
     *
     * @param callback a callback for notification when the download has finished
     */
    public void loadAsync(Callback callback) {
        onStartDownload();

        if (callback != null) {
            callback.reset();
        }

        currentTry = 0;

        loadAsyncImpl(callback);
    }

    private void loadAsyncImpl(final Callback callback) {
        if (url == null) {
            Logger.e("No url specified");
            maxCacheTime = -1;
            if (callback != null) {
                callback.error();
            }
            return;
        }

        Logger.i("Starting download (%d) %s", currentTry + 1, url);

        if (isDataLoaded() && requestUrl.equals(url)) {
            if (callback != null) {
                callback.success(true);
            }
            Logger.i("%s is up to date", url);
            return;
        }

        final Request.Builder rb = new Request.Builder();

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            rb.addHeader(entry.getKey(), entry.getValue());
        }

        Request request;
        try {
            request = rb.url(url).build();
        } catch (IllegalArgumentException e) {
            Logger.e("No url specified");
            maxCacheTime = -1;
            if (callback != null) {
                callback.success(true);
            }
            return;
        }

        synchronized (lock) {
            if (client == null) {
                throw new IllegalStateException("Downloader is not correctly configured.");
            }

            currentTry++;
            fromCache = false;
            call = client.newCall(request);
            call.enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull final Call call, @NonNull final IOException e) {
                    if (call.isCanceled()) {
                        Logger.i("Canceled download %s", url);
                    } else {
                        retry(callback);
                    }
                }

                @Override
                public void onResponse(@NonNull final Call call, @NonNull final Response response) {
                    try {
                        if (!response.isSuccessful()) {
                            fail(callback, response);
                            Logger.e("Failed to download (%d) %s", response.code(), url);
                            response.close();
                            return;
                        }

                        if (!isDataLoaded() || !requestUrl.equals(url)) {
                            fromCache = response.cacheResponse() != null;
                            if (fromCache) {
                                Logger.d("Response of %s from cache ", url);
                            } else {
                                Logger.d("Response of %s from network ", url);
                            }

                            int maxAgeSeconds = response.cacheControl().maxAgeSeconds();
                            long responseTimeMs = Long.parseLong(response.header("OkHttp-Received-Millis", "0"));
                            if (responseTimeMs == 0) {
                                responseTimeMs = System.currentTimeMillis();
                            }

                            Downloader.this.onResponse(response);

                            maxCacheTime = responseTimeMs + TimeUnit.SECONDS.toMillis(
                                    inMemoryCacheTime > 0 ? inMemoryCacheTime : maxAgeSeconds);

                            requestUrl = url;

                            synchronized (lock) {
                                Downloader.this.call = null;
                            }

                            if (callback != null) {
                                callback.success(false);
                            }

                            Logger.i("Successfully downloaded %s", url);
                        } else {
                            if (callback != null) {
                                callback.success(false);
                            }

                            Logger.i("%s is up to date", url);
                        }
                    } catch (IOException e) {
                        Logger.d(e.getMessage());
                        retry(callback);
                    }

                    response.close();
                }
            });
        }
    }

    public void cancel() {
        synchronized (lock) {
            if (call != null) {
                call.cancel();
                call = null;
            }
        }
    }

    public boolean isLoading() {
        synchronized (lock) {
            return call != null;
        }
    }

    private void retry(final Callback callback) {
        if (currentTry < retryCount || retryCount == -1) {
            Logger.i("Retrying download %s", url);
            handler.postDelayed(() -> loadAsyncImpl(callback), 2000);
        } else {
            Logger.i("Download of %s failed after %d attempts", url, retryCount + 1);
            fail(callback, null);
        }
    }

    private void fail(Callback callback, Response response) {
        synchronized (lock) {
            Downloader.this.call = null;
        }

        onDownloadFailed(response);

        if (callback != null) {
            callback.error();
        }
    }

    /**
     * Gets called when the download is about to be started
     */
    protected void onStartDownload() {}

    /**
     * Gets called when the download from loadAsync failed (for whatever reason).
     * <p>
     * Response can be null if the download was interrupted
     */
    protected void onDownloadFailed(Response response) {
        // Handle if needed.
    }

    /**
     * Gets called when the download from loadAsync is finished
     */
    protected abstract void onResponse(Response response) throws IOException;

    public boolean isDataLoaded() {
        return maxCacheTime != -1 && System.currentTimeMillis() < maxCacheTime;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    /**
     * When set to a value higher than 0, it uses this time in seconds as cache time.
     */
    public void setInMemoryCacheTime(int seconds) {
        inMemoryCacheTime = seconds;
    }

    public void invalidate() {
        maxCacheTime = -1;
        requestUrl = null;
    }

    /**
     * Sets the number of times a download should be retried on failure.
     * <p>
     * Default value is 10.
     * <p>
     * Set to -1 for infinite amount of retries.
     */
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * Abstract class for the callbacks.
     */
    public static abstract class Callback {
        boolean success = false;
        boolean error = false;

        void reset() {
            success = false;
            error = false;
        }

        void success(final boolean wasStillValid) {
            if (!success && !error) {
                success = true;

                onDataLoaded(wasStillValid);
            }
        }

        void error() {
            if (!error) {
                error = true;

                onError();
            }
        }

        /**
         * Called when data is loaded.
         *
         * @param wasStillValid Indicates whether the data has not changed since the last invocation
         */
        protected void onDataLoaded(boolean wasStillValid) {}

        /**
         * Called when there is an error loading the data type.
         */
        protected void onError() {}
    }
}