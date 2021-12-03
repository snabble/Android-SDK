package io.snabble.sdk.ui.fragment;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class ZebraSupport {
    public static @Nullable String dispatchKeyEvent(Activity activity, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getScanCode() == 310) {
            // ACTION_MULTIPLE gets consumed by if a focused view has an active InputConnection
            View v = activity.getCurrentFocus();
            if (v != null) {
                v.clearFocus();
            }
        } else if (event.getAction() == KeyEvent.ACTION_MULTIPLE) {
            return event.getCharacters();
        }

        return null;
    }
}
