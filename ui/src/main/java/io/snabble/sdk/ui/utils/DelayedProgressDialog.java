package io.snabble.sdk.ui.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class DelayedProgressDialog extends ProgressDialog {
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private boolean isDismissed = false;

    public DelayedProgressDialog(Context context) {
        super(context);
    }

    public void showAfterDelay(long afterDelayMs) {
        runnable = new Runnable() {
            public void run() {
                if(!isDismissed) {
                    show();
                }
            }
        };

        isDismissed = false;
        handler.postDelayed(runnable, afterDelayMs);
    }

    @Override
    public void hide() {
        stopCallbacks();
        super.hide();
    }

    @Override
    public void cancel() {
        stopCallbacks();
        super.cancel();
    }

    private void stopCallbacks() {
        isDismissed = true;
        handler.removeCallbacks(runnable);
        setOnCancelListener(null);
    }

    @Override
    public void dismiss() {
        stopCallbacks();
        super.dismiss();
    }
}