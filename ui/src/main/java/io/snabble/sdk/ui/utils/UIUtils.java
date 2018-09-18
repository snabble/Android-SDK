package io.snabble.sdk.ui.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

public class UIUtils {
    public static final int SNACKBAR_LENGTH_VERY_LONG = 5000;

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

    public static void setColoredText(TextView tv, String text, String subText, int color) {
        SpannableString spannableString = new SpannableString(text);
        ForegroundColorSpan foregroundSpan = new ForegroundColorSpan(color);
        int start = text.lastIndexOf(subText);
        int end = start + subText.length();
        spannableString.setSpan(foregroundSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(spannableString);
    }
}
