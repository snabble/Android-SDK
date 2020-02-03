package io.snabble.sdk.ui.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;

import io.snabble.sdk.ui.R;

public class UIUtils {
    public static final int INFO_NEUTRAL = 0;
    public static final int INFO_WARNING = 1;
    public static final int INFO_POSITIVE = 2;

    public static final int SNACKBAR_LENGTH_VERY_LONG = 5000;

    public static Snackbar snackbar(View view, @StringRes int stringResId, int duration) {
        return Snackbar.make(view, stringResId, duration);
    }

    public static Snackbar snackbar(View view, String text, int duration) {
        return Snackbar.make(view, text, duration);
    }

    public static FragmentActivity getHostFragmentActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (FragmentActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }

        return null;
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

    public static int getDurationByLength(String text) {
        return Math.max(Math.min(text.length() * 70, 2000), 7000);
    }

    public static int getColorByAttribute(Context context, @AttrRes int attrResId) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrResId, typedValue, true);
        return typedValue.data;
    }
    
    public static View showTopDownInfoBox(ViewGroup parent, String text, int duration, int type) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final TextView info = (TextView)inflater.inflate(R.layout.snabble_view_info, null);

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
            case INFO_NEUTRAL:
                info.setBackgroundColor(ResourcesCompat.getColor(res, R.color.snabble_infoColor, null));
                info.setTextColor(ResourcesCompat.getColor(res, R.color.snabble_infoTextColor, null));
                break;
            case INFO_WARNING:
                info.setBackgroundColor(ResourcesCompat.getColor(res, R.color.snabble_infoColorWarning, null));
                info.setTextColor(ResourcesCompat.getColor(res, R.color.snabble_infoTextColorWarning, null));
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
