package io.snabble.testapp;

import android.app.Application;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;

import io.snabble.sdk.Config;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.telemetry.Telemetry;

public class App extends Application {
    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        //you may enable debug logging to see requests made by the sdk, and other various logs
        Snabble.setDebugLoggingEnabled(true);

        // sets a ui event listener for telemetry events, which can you redirect to any
        // telemetry provider
        Telemetry.setOnEventListener((event, data) -> {
            String dataStr = "";

            if (data != null) {
                dataStr = data.toString();
            }

            Log.d("Telemetry", String.format("Event: %s [%s]", event.toString(), dataStr));
        });
    }

    public Project getProject() {
        return Snabble.getInstance().getCheckedInProject().getValue();
    }

    private int getBundledRevisionId(String projectId) {
        try {
            return Integer.parseInt(IOUtils.toString(getAssets().open(projectId + ".revision"),
                    Charset.forName("UTF-8")));
        } catch (IOException e) {
            return -1;
        }
    }

    private int getBundledMajor(String projectId) {
        try {
            return Integer.parseInt(IOUtils.toString(getAssets().open(projectId + ".major"),
                    Charset.forName("UTF-8")));
        } catch (IOException e) {
            return -1;
        }
    }

    private int getBundledMinor(String projectId) {
        try {
            return Integer.parseInt(IOUtils.toString(getAssets().open(projectId + ".minor"),
                    Charset.forName("UTF-8")));
        } catch (IOException e) {
            return -1;
        }
    }

    public static App get() {
        return instance;
    }
}