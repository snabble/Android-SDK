package io.snabble.testapp;

import android.app.Application;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import io.snabble.sdk.Config;
import io.snabble.sdk.Project;
import io.snabble.sdk.Snabble;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.telemetry.Telemetry;

public class App extends Application {
    private static App instance;

    private Project project;

    public interface InitCallback {
        void done();

        void error(String text);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        //you may enable debug logging to see requests made by the sdk, and other various logs
        Snabble.setDebugLoggingEnabled(true);

        // config {
        Config config = new Config();
        config.endpointBaseUrl = getString(R.string.endpoint);
        config.secret = getString(R.string.secret);
        config.appId = getString(R.string.app_id);
        // }

        final Snabble snabble = Snabble.getInstance();
        snabble.setup(this, config, new Snabble.SetupCompletionListener() {
            @Override
            public void onReady() {
                SnabbleUI.setProject(snabble.getProjects().get(0));
            }

            @Override
            public void onError(Snabble.Error error) {

            }
        });

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
        return SnabbleUI.getProject();
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