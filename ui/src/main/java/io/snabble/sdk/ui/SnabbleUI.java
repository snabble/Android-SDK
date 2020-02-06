package io.snabble.sdk.ui;


import androidx.appcompat.app.ActionBar;

import io.snabble.sdk.Project;
import io.snabble.sdk.utils.Logger;

public class SnabbleUI {
    public enum Action {
        SHOW_PAYMENT_SELECTION,
        SHOW_CHECKOUT_ONLINE,
        SHOW_CHECKOUT_OFFLINE,
        SHOW_CHECKOUT_POINT_OF_SALE,
        SHOW_CHECKOUT_GATEKEEPER,
        SHOW_PAYMENT_SUCCESS,
        SHOW_PAYMENT_FAILURE,
        SHOW_SCANNER,
        SHOW_BARCODE_SEARCH,
        SHOW_SEPA_CARD_INPUT,
        SHOW_CREDIT_CARD_INPUT,
        SHOW_SHOPPING_CART,
        SHOW_PAYMENT_CREDENTIALS_LIST,
        GO_BACK
    }

    public interface Callback {
        void execute(Action action, Object data);
    }

    private static Project project;
    private static SnabbleUI.Callback uiCallback;
    private static ActionBar actionBar;

    /**
     * Registers a globally used project for use with views.
     * <p>
     */
    public static void useProject(Project project) {
        SnabbleUI.project = project;
    }

    /**
     * Registers a callback that the application needs to implement and respond accordingly.
     * <p>
     * Remember to unregister the callback when not used anymore, for example if you implement
     * this callback in an Activity onStart, remember to call unregisterUiCallbacks in onStop()
     * <p>
     * Calling this multiple times overwrites the old callback
     */
    public static void registerUiCallbacks(SnabbleUI.Callback callback) {
        uiCallback = callback;
    }

    /**
     * Unregisters a callback that was previously registered.
     */
    public static void unregisterUiCallbacks(SnabbleUI.Callback callback) {
        if (uiCallback == callback) {
            uiCallback = null;
        }
    }

    public static SnabbleUI.Callback getUiCallback() {
        if (uiCallback == null) {
            Logger.e("Could not perform user interface action: SnabbleUICallback is null.");
        }

        return uiCallback;
    }

    /**
     * Registers a ActionBar for suggested changes on the action bar.
     * <p>
     * Remember to unregister the ActionBar when not used anymore, for example if you set
     * this ActionBar in an Activity onCreate, remember to call unregisterActionBar in onDestroy()
     * <p>
     * Calling this multiple times overwrites the old action bar
     */
    public static void registerActionBar(ActionBar bar) {
        actionBar = bar;
    }

    /**
     * Unregisters a ActionBar that was previously registered.
     */
    public static void unregisterActionBar(ActionBar bar) {
        if (actionBar == bar) {
            actionBar = null;
        }
    }

    public static ActionBar getActionBar() {
        return actionBar;
    }

    public static Project getProject() {
        if (project == null) {
            throw new RuntimeException("No Project instance set." +
                    " Use SnabbleUI.registerProject after SDK initialization.");
        }

        return project;
    }
}
