package io.snabble.sdk.ui.utils;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;

import io.snabble.sdk.Snabble;

public class KeyguardUtils {
    public static boolean isDeviceSecure() {
        Context context = Snabble.getInstance().getApplication();
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        if (keyguardManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return keyguardManager.isDeviceSecure(); // ignores SIM lock, API 23+
        } else {
            return keyguardManager.isKeyguardSecure();
        }
    }
}
