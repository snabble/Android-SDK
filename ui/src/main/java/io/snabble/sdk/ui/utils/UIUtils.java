package io.snabble.sdk.ui.utils;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.AttrRes;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.snabble.sdk.ui.R;
import kotlin.Deprecated;
import kotlin.ReplaceWith;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

public class UIUtils {
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({INFO_NEUTRAL, INFO_WARNING, INFO_POSITIVE})
    @IntRange(from = 1)
    @Retention(RetentionPolicy.SOURCE)
    public @interface InfoLevel {}

    public static final int INFO_NEUTRAL = 0;
    public static final int INFO_WARNING = 1;
    public static final int INFO_POSITIVE = 2;

    public static final int SNACKBAR_LENGTH_VERY_LONG = 5000;

    public static FragmentActivity getHostFragmentActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof FragmentActivity) {
                return (FragmentActivity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }

        return null;
    }

    public static ComponentActivity getHostComponentActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof ComponentActivity) {
                return (ComponentActivity) context;
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

    public static View showTopDownInfoBox(ViewGroup parent, String text, int duration, int type, int offsetY) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final TextView info = (TextView)inflater.inflate(R.layout.snabble_view_info, parent, false);

        parent.addView(info, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        info.setVisibility(View.INVISIBLE);
        info.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                info.removeOnLayoutChangeListener(this);

                info.setTranslationY(offsetY - info.getHeight());
                info.setVisibility(View.VISIBLE);
                info.animate().translationY(offsetY).start();
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
        infoHandler.postDelayed(() -> info.animate()
                .translationY(-offsetY - info.getHeight())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        parent.removeView(info);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        parent.removeView(info);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                }).start(), duration);

        return info;
    }
}
