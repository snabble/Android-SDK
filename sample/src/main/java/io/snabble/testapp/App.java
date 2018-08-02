package io.snabble.testapp;

import android.app.Application;
import android.support.annotation.Nullable;
import android.util.Log;

import com.squareup.leakcanary.LeakCanary;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;

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

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        instance = this;
    }

    public void init(final InitCallback callback) {
        if(project != null){
            callback.done();
            return;
        }

        //you may enable debug logging to see requests made by the sdk, and other various logs
        Snabble.setDebugLoggingEnabled(true);

        Snabble.Config config = new Snabble.Config();
        config.endpointBaseUrl = getString(R.string.endpoint);
        config.clientToken = getString(R.string.client_token);
        config.appId = getString(R.string.app_id);
        config.bundledMetadataAssetPath = "metadata.json";

        final Snabble snabble = Snabble.getInstance();
        snabble.setup(this, config, new Snabble.SetupCompletionListener() {
            @Override
            public void onReady() {
                project = snabble.getProjects().get(0);

                // registers this sdk instance globally for use with ui components
                SnabbleUI.registerProject(project);

                // select the first shop for demo purposes
                if (project.getShops().length > 0) {
                    project.getCheckout().setShop(project.getShops()[0]);
                }

                // optionally set a loyalty card id for identification, for demo purposes
                // we invent one here
                project.setLoyaltyCardId("testAppUserLoyaltyCardId");

                callback.done();
            }

            @Override
            public void onError(Snabble.Error error) {
                callback.error("SdkError: " + error.toString());
            }
        });

        // sets a ui event listener for telemetry events, which can you redirect to any
        // telemetry provider
        Telemetry.setOnEventListener(new Telemetry.OnEventListener() {
            @Override
            public void onEvent(Telemetry.Event event, @Nullable Object data) {
                String dataStr = "";

                if (data != null) {
                    dataStr = data.toString();
                }

                Log.d("Telemetry", String.format("Event: %s [%s]", event.toString(), dataStr));
            }
        });
    }

    public Project getProject() {
        return project;
    }

    private int getBundledRevisionId() {
        try {
            return Integer.parseInt(IOUtils.toString(getAssets().open("products.revision"),
                    Charset.forName("UTF-8")));
        } catch (IOException e) {
            return -1;
        }
    }

    private int getBundledMajor() {
        try {
            return Integer.parseInt(IOUtils.toString(getAssets().open("products.major"),
                    Charset.forName("UTF-8")));
        } catch (IOException e) {
            return -1;
        }
    }

    private int getBundledMinor() {
        try {
            return Integer.parseInt(IOUtils.toString(getAssets().open("products.minor"),
                    Charset.forName("UTF-8")));
        } catch (IOException e) {
            return -1;
        }
    }

    public static App get() {
        return instance;
    }
}
