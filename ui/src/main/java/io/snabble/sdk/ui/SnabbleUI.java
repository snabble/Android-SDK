package io.snabble.sdk.ui;


import androidx.appcompat.app.ActionBar;
import io.snabble.sdk.Project;
import io.snabble.sdk.utils.Logger;

public class SnabbleUI {
    private static Project project;
    private static SnabbleUICallback uiCallback;
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
