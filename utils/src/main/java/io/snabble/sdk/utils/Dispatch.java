package io.snabble.sdk.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Dispatch {
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(0);
    private static final ScheduledExecutorService ioScheduler = Executors.newSingleThreadScheduledExecutor();

    private static MainThreadHandler mainThreadHandler;
    private static MainThreadHandler backgroundThreadHandler;

    public interface MainThreadHandler {
        void handle(Runnable runnable);
    }

    public static void setMainThreadHandler(MainThreadHandler handler) {
        Dispatch.mainThreadHandler = handler;
    }

    public static void setBackgroundThreadHandler(MainThreadHandler handler) {
        Dispatch.backgroundThreadHandler = handler;
    }

    public static Future<?> background(Runnable runnable) {
        return executorService.submit(runnable);
    }

    public static Future<?> io(Runnable runnable) {
        if (backgroundThreadHandler != null) {
            backgroundThreadHandler.handle(runnable);
            return new Future<Void>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return true;
                }

                @Override
                public Void get() throws ExecutionException, InterruptedException {
                    return null;
                }

                @Override
                public Void get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
                    return null;
                }
            };
        }

        return ioScheduler.submit(runnable);
    }

    public static Future<?> background(Runnable runnable, long delayMs) {
        if (backgroundThreadHandler != null) {
            backgroundThreadHandler.handle(runnable);
            return new Future<Void>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return false;
                }

                @Override
                public boolean isCancelled() {
                    return false;
                }

                @Override
                public boolean isDone() {
                    return false;
                }

                @Override
                public Void get() throws ExecutionException, InterruptedException {
                    return null;
                }

                @Override
                public Void get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
                    return null;
                }
            };
        }

        return executorService.schedule(runnable, delayMs, TimeUnit.MILLISECONDS);
    }

    public static void mainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            if (mainThreadHandler != null) {
                mainThreadHandler.handle(runnable);
                return;
            }

            handler.post(runnable);
        }
    }

    public static void mainThread(Runnable runnable, long delayMs) {
        handler.postDelayed(runnable, delayMs);
    }
}