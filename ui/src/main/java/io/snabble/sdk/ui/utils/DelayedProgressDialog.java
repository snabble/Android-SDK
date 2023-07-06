package io.snabble.sdk.ui.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class DelayedProgressDialog extends ProgressDialog {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable showRunnable = new Runnable() {
        public void run() {
            if (!isDismissed) {
                show();
            }
        }
    };

    private boolean isDismissed = false;

    public DelayedProgressDialog(Context context) {
        super(context);
    }

    public void showAfterDelay(long afterDelayMs) {
        stopCallbacks();

        isDismissed = false;
        handler.postDelayed(showRunnable, afterDelayMs);
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
        handler.removeCallbacks(showRunnable);
        setOnCancelListener(null);
    }

    @Override
    public void dismiss() {
        stopCallbacks();
        super.dismiss();
    }
}
