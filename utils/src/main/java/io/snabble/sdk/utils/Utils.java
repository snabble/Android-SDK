package io.snabble.sdk.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
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
}
