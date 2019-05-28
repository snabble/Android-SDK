package io.snabble.testapp;

import android.app.Application;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.squareup.leakcanary.LeakCanary;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import io.snabble.sdk.Checkout;
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
        createNotificationChannel();

        instance = this;
    }

    public void initBlocking() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        init(new InitCallback() {
            @Override
            public void done() {
                countDownLatch.countDown();
            }

            @Override
            public void error(String text) {
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        config.secret = getString(R.string.secret);
        config.appId = getString(R.string.app_id);

        final Snabble snabble = Snabble.getInstance();
        snabble.setup(this, config, new Snabble.SetupCompletionListener() {
            @Override
            public void onReady() {
                project = snabble.getProjects().get(0);

                // registers this project globally for use with ui components
                SnabbleUI.useProject(project);

                // select the first shop for demo purposes
                if (project.getShops().length > 0) {
                    project.setCheckedInShop(project.getShops()[0]);
                }

                // you can update the local database asynchronously, you can still query
                // the database while this is running
                // project.getProductDatabase().update();

                // optionally set a loyalty card id for identification, for demo purposes
                // we invent one here
                project.setCustomerCardId("testAppUserLoyaltyCardId");

                // if you want to force keyguard authentication before online payment
                snabble.getUserPreferences().setRequireKeyguardAuthenticationForPayment(true);

                for (final Project project : snabble.getProjects()) {
                    project.getCheckout().addOnCheckoutStateChangedListener(new Checkout.OnCheckoutStateChangedListener() {
                        @Override
                        public void onStateChanged(Checkout.State state) {
                            if (state == Checkout.State.RECEIPT_AVAILABLE) {
                                Intent intent = new Intent(getApplicationContext(), ReceiptListActivity.class);
                                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

                                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "0")
                                        .setSmallIcon(R.mipmap.ic_launcher)
                                        .setContentTitle(project.getCheckout().getShop().getName())
                                        .setContentText("Your Receipt is ready.")
                                        .setContentIntent(pendingIntent)
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

                                notificationManager.notify(0, builder.build());
                            }
                        }
                    });
                }

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

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "name";
            String description = "desc";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("0", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
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
