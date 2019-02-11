package io.snabble.sdk.ui.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;
import io.snabble.sdk.ui.R;

import com.google.android.material.snackbar.Snackbar;

import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class UIUtils {
    public static final int INFO_WARNING = 0;
    public static final int INFO_POSITIVE = 1;

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

            TextView textView = v.findViewById(com.google.android.material.R.id.snackbar_text);
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

    public static View showTopDownInfoBox(ViewGroup parent, String text, int duration, int type) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final TextView info = (TextView)inflater.inflate(R.layout.view_info, null);

        parent.addView(info, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        info.setVisibility(View.INVISIBLE);
        info.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                info.removeOnLayoutChangeListener(this);

                info.setTranslationY(-info.getHeight());
                info.setVisibility(View.VISIBLE);
                info.animate().translationY(0).start();
            }
        });

        Resources res = parent.getContext().getResources();
        switch (type) {
            case INFO_WARNING:
                info.setBackgroundColor(ResourcesCompat.getColor(res, R.color.snabble_infoColor, null));
                info.setTextColor(ResourcesCompat.getColor(res, R.color.snabble_infoTextColor, null));
                break;
            case INFO_POSITIVE:
                info.setBackgroundColor(ResourcesCompat.getColor(res, R.color.snabble_infoColorPositive, null));
                info.setTextColor(ResourcesCompat.getColor(res, R.color.snabble_infoTextColorPositive, null));
                break;
        }

        info.setText(text);

        Handler infoHandler = new Handler(Looper.getMainLooper());
        infoHandler.removeCallbacksAndMessages(null);
        infoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                info.animate().translationY(-info.getHeight()).start();
            }
        }, duration);

        return info;
    }
}
