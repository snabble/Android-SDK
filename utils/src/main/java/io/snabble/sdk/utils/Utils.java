package io.snabble.sdk.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import androidx.appcompat.app.AppCompatActivity;

public class Utils {
    public static Activity getActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                return activity;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public static AppCompatActivity getAppCompatActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) context;
                return activity;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }

        return null;
    }

    public static int dp2px(Context context, float dp) {
        return Math.round(context.getResources().getDisplayMetrics().density * dp);
    }

    public static float px2dp(Context context, int px) {
        return (float) px / context.getResources().getDisplayMetrics().density;
    }

    public static byte[] sha1(String input) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }

        return messageDigest.digest(input.getBytes(Charset.forName("UTF-8")));
    }

    public static byte[] sha256(String input) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }

        return messageDigest.digest(input.getBytes(Charset.forName("UTF-8")));
    }

    public static String sha1Hex(String input) {
        return hexString(sha1(input));
    }

    public static String sha256Hex(String input) {
        return hexString(sha256(input));
    }

    public static String hexString(byte[] input) {
        StringBuilder sb = new StringBuilder();
        for (byte b : input) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static boolean isDebugBuild(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
