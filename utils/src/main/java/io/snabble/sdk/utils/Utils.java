package io.snabble.sdk.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

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

    public static String sha1Hex(String input) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }

        byte[] result = messageDigest.digest(input.getBytes(Charset.forName("UTF-8")));
        StringBuilder sb = new StringBuilder();
        for (byte b : result) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
