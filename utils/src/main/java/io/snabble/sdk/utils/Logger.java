package io.snabble.sdk.utils;

import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper class for lazy logging
 */
public class Logger {
    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");
    private static final int CALL_STACK_INDEX = 2;

    private static boolean isEnabled = BuildConfig.DEBUG;

    public static void setEnabled(boolean enable) {
        isEnabled = enable;
    }

    private static String getTag() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length <= CALL_STACK_INDEX) {
            return Logger.class.getSimpleName();
        }

        StackTraceElement stackTraceElement = stackTrace[CALL_STACK_INDEX];
        String className = stackTraceElement.getClassName();
        Matcher m = ANONYMOUS_CLASS.matcher(className);
        if (m.find()) {
            className = m.replaceAll("");
        }

        String tag = className.substring(className.lastIndexOf('.') + 1);
        int lineNumber = stackTraceElement.getLineNumber();
        return "(" + tag + ".java:" + lineNumber + ")";
    }

    public static void i(String message, Object... args) {
        if (isEnabled && message != null) {
            Log.i(getTag(), String.format(message, args));
        }
    }

    public static void d(String message, Object... args) {
        if (isEnabled && message != null) {
            Log.d(getTag(), String.format(message, args));
        }
    }

    public static void w(String message, Object... args) {
        if (isEnabled && message != null) {
            Log.w(getTag(), String.format(message, args));
        }
    }

    public static void e(String message, Object... args) {
        if (isEnabled && message != null) {
            Log.e(getTag(), String.format(message, args));
        }
    }

    public static void v(String message, Object... args) {
        if (isEnabled && message != null) {
            Log.v(getTag(), String.format(message, args));
        }
    }
}
