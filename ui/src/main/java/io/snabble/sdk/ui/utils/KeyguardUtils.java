package io.snabble.sdk.ui.utils;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;

import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.KeyguardHandler;
import io.snabble.sdk.ui.SnabbleUI;

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

    public static void showKeyguard(KeyguardHandler keyguardHandler) {
        if (Snabble.getInstance().getUserPreferences().isRequiringKeyguardAuthenticationForPayment()) {
            SnabbleUI.getUiCallback().requestKeyguard(keyguardHandler);
        } else {
            keyguardHandler.onKeyguardResult(Activity.RESULT_OK);
        }
    }
}
