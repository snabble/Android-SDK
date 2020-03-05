package io.snabble.sdk.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Dispatch {
    private static Handler handler = new Handler(Looper.getMainLooper());
    private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(0);

    public static Future<?> background(Runnable runnable) {
        return executorService.submit(runnable);
    }

    public static Future<?> background(Runnable runnable, long delayMs) {
        return executorService.schedule(runnable, delayMs, TimeUnit.MILLISECONDS);
    }

    public static void mainThread(Runnable runnable) {
        handler.post(runnable);
    }

    public static void mainThread(Runnable runnable, long delayMs) {
        handler.postDelayed(runnable, delayMs);
    }
}
