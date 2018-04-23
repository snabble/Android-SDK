package io.snabble.sdk.ui.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

public class UIUtils {
    public static Snackbar snackbar(View view, @StringRes int stringResId, int duration) {
        Snackbar snackbar = Snackbar.make(view, stringResId, duration);
        fixTextColor(snackbar);
        return snackbar;
    }

    public static Snackbar snackbar(View view, String text, int duration) {
        Snackbar snackbar = Snackbar.make(view, text, duration);
        fixTextColor(snackbar);
        return snackbar;
    }

    private static void fixTextColor(Snackbar snackbar) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            View v = snackbar.getView();

            TextView textView = v.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(Color.parseColor("#ffffff"));
        }
    }

    public static Activity getHostActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }

        return null;
    }
}
