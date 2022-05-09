package io.snabble.sdk.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Dispatch {
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(0);
    private static final ScheduledExecutorService ioScheduler = Executors.newSingleThreadScheduledExecutor();

    private static MainThreadHandler mainThreadHandler;

    public interface MainThreadHandler {
        void handle(Runnable runnable);
    }

    public static void setMainThreadHandler(MainThreadHandler runnable) {
        Dispatch.mainThreadHandler = runnable;
    }

    public static Future<?> background(Runnable runnable) {
        return executorService.submit(() -> {
            try {
                runnable.run();
            } catch (Throwable t){
                Log.e("Dispatch", "Crash on background thread", t);
                throw t;
            }
        });
    }

    public static Future<?> io(Runnable runnable) {
        return ioScheduler.submit(() -> {
            try {
                runnable.run();
            } catch (Throwable t){
                Log.e("Dispatch", "Crash on I/O thread", t);
                throw t;
            }
        });
    }

    public static Future<?> background(Runnable runnable, long delayMs) {
        return executorService.schedule(() -> {
            try {
                runnable.run();
            } catch (Throwable t){
                Log.e("Dispatch", "Crash on background thread", t);
                throw t;
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public static void mainThread(Runnable runnable) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                runnable.run();
            } else {
                if (mainThreadHandler != null) {
                    mainThreadHandler.handle(() -> {
                        try {
                            runnable.run();
                        } catch (Throwable t){
                            Log.e("Dispatch", "Crash on main thread", t);
                            throw t;
                        }
                    });
                    return;
                }

                handler.post(runnable);
            }
        } catch (Throwable t){
            Log.e("Dispatch", "Crash on main thread", t);
            throw t;
        }
    }

    public static void mainThread(Runnable runnable, long delayMs) {
        handler.postDelayed(runnable, delayMs);
    }
}