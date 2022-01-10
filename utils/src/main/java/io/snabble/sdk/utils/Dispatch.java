package io.snabble.sdk.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Dispatch {
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(0);
    private static final ScheduledExecutorService ioScheduler = Executors.newSingleThreadScheduledExecutor();

    public static Future<?> background(Runnable runnable) {
        return executorService.submit(runnable);
    }

    public static Future<?> io(Runnable runnable) {
        return executorService.submit(runnable);
    }

    public static Future<?> background(Runnable runnable, long delayMs) {
        return executorService.schedule(runnable, delayMs, TimeUnit.MILLISECONDS);
    }

    public static void mainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    public static void mainThread(Runnable runnable, long delayMs) {
        handler.postDelayed(runnable, delayMs);
    }
}