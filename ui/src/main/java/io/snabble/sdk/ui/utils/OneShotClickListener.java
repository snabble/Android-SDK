package io.snabble.sdk.ui.utils;

import android.os.SystemClock;
import android.view.View;

public abstract class OneShotClickListener implements View.OnClickListener {
    private long lastClickTime = 0;

    @Override
    public void onClick(View v) {
        long time = SystemClock.elapsedRealtime();

        if(time - lastClickTime > 300) {
            lastClickTime = SystemClock.elapsedRealtime();
            click();
        }
    }

    public abstract void click();
}
