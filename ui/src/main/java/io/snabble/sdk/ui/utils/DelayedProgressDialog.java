package io.snabble.sdk.ui.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class DelayedProgressDialog extends ProgressDialog {
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;

    public DelayedProgressDialog(Context context) {
        super(context);
    }

    public void showAfterDelay(long afterDelayMs) {
        runnable = new Runnable() {
            public void run() {
                show();
            }
        };

        handler.postDelayed(runnable, afterDelayMs);
    }

    @Override
    public void hide() {
        handler.removeCallbacks(runnable);
        setOnCancelListener(null);
        super.hide();
    }

    @Override
    public void cancel() {
        handler.removeCallbacks(runnable);
        setOnCancelListener(null);
        super.cancel();
    }

    @Override
    public void dismiss() {
        handler.removeCallbacks(runnable);
        setOnCancelListener(null);
        super.dismiss();
    }
}