package io.snabble.sdk.ui;


import android.app.Application;

import java.lang.ref.WeakReference;

import io.snabble.sdk.SnabbleSdk;
import io.snabble.sdk.utils.Logger;

public class SnabbleUI {
    private static SnabbleSdk sdkInstance;
    private static SnabbleUICallback uiCallback;

    /**
     * Registers a globally used sdk instance for use with views.
     * <p>
     * Should be called in the {@link SnabbleSdk#setup(Application, SnabbleSdk.Config,
     * SnabbleSdk.SetupCompletionListener)} callback.
     */
    public static void registerSdkInstance(SnabbleSdk sdkInstance) {
        SnabbleUI.sdkInstance = sdkInstance;
    }

    /**
     * Registers a callback that the application needs to implement and respond accordingly.
     * <p>
     * Remember to unregister the callback when not used anymore, for example if you implement
     * this callback in an Activity onCreate, remember to call unregisterUiCallbacks in onDestroy()
     * <p>
     * Calling this multiple times overwrites the old callback
     */
    public static void registerUiCallbacks(SnabbleUICallback callback) {
        uiCallback = callback;
    }

    /**
     * Unregisters a callback that was previously registered.
     */
    public static void unregisterUiCallbacks(SnabbleUICallback callback) {
        if (uiCallback == callback) {
            uiCallback = null;
        }
    }

    public static SnabbleUICallback getUiCallback() {
        if (uiCallback == null) {
            Logger.e("Could not perform user interface action: SnabbleUICallback is null.");
        }

        return uiCallback;
    }

    public static SnabbleSdk getSdkInstance() {
        if (sdkInstance == null) {
            throw new RuntimeException("No SnabbleSdk instance set." +
                    " Use SnabbleUI.registerSdkInstance after Sdk initialization.");
        }

        return sdkInstance;
    }
}
