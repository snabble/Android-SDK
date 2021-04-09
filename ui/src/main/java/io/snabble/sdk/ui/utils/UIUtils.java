package io.snabble.sdk.ui.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.annotation.AttrRes;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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

    @BaseTransientBottomBar.Duration
    public static final int SNACKBAR_LENGTH_VERY_LONG = 5000;

    @Deprecated(message = "Use Snackbar.make() instead", replaceWith = @ReplaceWith(expression = "Snackbar.make()", imports = "com.google.android.material.snackbar.Snackbar"))
    public static Snackbar snackbar(View view, @StringRes int stringResId, int duration) {
        return Snackbar.make(view, stringResId, duration);
    }

    @Deprecated(message = "Use Snackbar.make() instead", replaceWith = @ReplaceWith(expression = "Snackbar.make()", imports = "com.google.android.material.snackbar.Snackbar"))
    public static Snackbar snackbar(View view, String text, int duration) {
        return Snackbar.make(view, text, duration);
    }

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

    @Deprecated(message = "Replace with Snackbar")
    public static View showTopDownInfoBox(ViewGroup parent, String text, int duration, @InfoLevel int type) {
        KotlinExtensionsKt.setGravity(KotlinExtensionsKt.setPriority(Snackbar.make(parent, text, duration), type), Gravity.TOP).show();
        return null; // null as placeholder
    }
}
