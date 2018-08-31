package io.snabble.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import io.snabble.sdk.auth.TokenRegistry;
import io.snabble.sdk.utils.Downloader;
import io.snabble.sdk.utils.JsonUtils;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import okhttp3.OkHttpClient;

public class Snabble {
    private static Snabble instance = new Snabble();

    private List<Project> projects;
    private OkHttpClient okHttpClient;
    private TokenRegistry tokenRegistry;
    private Application application;
    private MetadataDownloader metadataDownloader;
    private UserPreferences userPreferences;
    private File internalStorageDirectory;
    private String metadataUrl;
    private Config config;
    private List<OnMetadataUpdateListener> onMetaDataUpdateListeners = new CopyOnWriteArrayList<>();
    private String versionName;

    private Snabble() {

    }

    public void setup(Application app, Config config, final SetupCompletionListener setupCompletionListener) {
        this.application = app;
        this.config = config;

        if (config.appId == null || config.secret == null) {
            setupCompletionListener.onError(Error.CONFIG_PARAMETER_MISSING);
            return;
        }

        okHttpClient = OkHttpClientFactory.createOkHttpClient(app, null);
        tokenRegistry = new TokenRegistry(okHttpClient, config.appId, config.secret);

        userPreferences = new UserPreferences(app);

        internalStorageDirectory = new File(application.getFilesDir(), "snabble/" + config.appId + "/");
        //noinspection ResultOfMethodCallIgnored
        internalStorageDirectory.mkdirs();

        projects = Collections.unmodifiableList(new ArrayList<Project>());

        if (config.endpointBaseUrl == null) {
            config.endpointBaseUrl = "https://api.snabble.io";
        } else if (!config.endpointBaseUrl.startsWith("http://") && !config.endpointBaseUrl.startsWith("https://")) {
            config.endpointBaseUrl = "https://" + config.endpointBaseUrl;
        }

        String version = config.versionName;
        if (version == null) {
            try {
                PackageInfo pInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                if (pInfo != null && pInfo.versionName != null) {
                    version = pInfo.versionName.toLowerCase(Locale.ROOT).replace(" ", "");
                } else {
                    version = "1.0";
                }
            } catch (PackageManager.NameNotFoundException e) {
                version = "1.0";
            }
        }

        versionName = version;
        metadataUrl = absoluteUrl("/metadata/app/" + config.appId + "/android/" + version);

        this.metadataDownloader = new MetadataDownloader(okHttpClient, config.bundledMetadataAssetPath);

        if (config.bundledMetadataAssetPath != null) {
            readMetadata();
            setupCompletionListener.onReady();
        } else {
            metadataDownloader.loadAsync(new Downloader.Callback() {
                @Override
                protected void onDataLoaded(boolean wasStillValid) {
                    readMetadata();
                    setupCompletionListener.onReady();
                }

                @Override
                protected void onError() {
                    if (metadataDownloader.hasData()) {
                        readMetadata();
                        setupCompletionListener.onReady();
                    } else {
                        setupCompletionListener.onError(Error.CONNECTION_TIMEOUT);
                    }
                }
            });
        }

        app.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    public String getVersionName() {
        return versionName;
    }

    /**
     * Return true when the SDK is not compatible with the backend anymore and the app should
     * notify the user that it will not function anymore.
     */
    public boolean isOutdatedSDK() {
        JsonObject jsonObject = metadataDownloader.getJsonObject();

        return jsonObject.has("metadata") &&
                JsonUtils.getBooleanOpt(jsonObject.get("metadata").getAsJsonObject(), "kill", false);
    }

    private synchronized void readMetadata() {
        JsonObject jsonObject = metadataDownloader.getJsonObject();
        if (jsonObject != null && jsonObject.has("projects")) {
            JsonArray jsonArray = jsonObject.get("projects").getAsJsonArray();
            List<Project> newProjects = new ArrayList<>();

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonProject = jsonArray.get(i).getAsJsonObject();

                // first try to find an already existing project and update it, so that
                // the object reference can be stored somewhere and still be up to date
                boolean updated = false;
                if (jsonProject.has("id")) {
                    for (Project p : projects) {
                        if (p.getId().equals(jsonProject.get("id").getAsString())) {
                            try {
                                p.parse(jsonProject);
                                newProjects.add(p);
                            } catch (IllegalArgumentException e) {
                                // malformed project, do nothing
                            }

                            updated = true;
                            break;
                        }
                    }

                    // if it does not exist, add it
                    if (!updated) {
                        try {
                            Project project = new Project(jsonProject);
                            newProjects.add(project);
                        } catch (IllegalArgumentException e) {
                            // malformed project, do nothing
                        }
                    }
                }
            }

            projects = Collections.unmodifiableList(newProjects);
        }
    }

    String absoluteUrl(String url) {
        if (url.startsWith("http")) {
            return url;
        } else {
            return getEndpointBaseUrl() + url;
        }
    }

    public String getEndpointBaseUrl() {
        return config.endpointBaseUrl;
    }

    String getMetadataUrl() {
        return metadataUrl;
    }

    File getInternalStorageDirectory() {
        return internalStorageDirectory;
    }

    Application getApplication() {
        return application;
    }

    public TokenRegistry getTokenRegistry() {
        return tokenRegistry;
    }

    public List<Project> getProjects() {
        return projects;
    }

    /**
     * The blocking version of {@link #setup(Application, Config, SetupCompletionListener)}
     * <p>
     * Blocks until every initialization is completed, that includes waiting for necessary
     * network calls if bundled data is not provided.
     * <p>
     * If all needed bundled data is provided (See {@link Config}), initialization requires
     * no network calls.
     *
     * @throws SnabbleException If an error occurs while initializing the sdk.
     */
    public void setupBlocking(Application app, Config config) throws SnabbleException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final Error[] snabbleError = new Error[1];

        setup(app, config, new SetupCompletionListener() {
            @Override
            public void onReady() {
                countDownLatch.countDown();
            }

            @Override
            public void onError(Error error) {
                snabbleError[0] = error;
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new SnabbleException(Error.UNSPECIFIED_ERROR);
        }

        if (snabbleError[0] != null) {
            throw new SnabbleException(snabbleError[0]);
        }
    }

    public static String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    private void updateMetadata() {
        metadataDownloader.setUrl(getMetadataUrl());
        metadataDownloader.loadAsync(new Downloader.Callback() {
            @Override
            protected void onDataLoaded(boolean wasStillValid) {
                if (!wasStillValid) {
                    readMetadata();

                    for (OnMetadataUpdateListener listener : onMetaDataUpdateListeners) {
                        listener.onMetaDataUpdated();
                    }
                }
            }
        });
    }

    /**
     * Adds a listener that gets called every time the metadata updates
     */
    public void addOnMetadataUpdateListener(OnMetadataUpdateListener onMetaDataUpdateListener) {
        onMetaDataUpdateListeners.add(onMetaDataUpdateListener);
    }

    /**
     * Removes an already added listener
     */
    public void removeOnMetadataUpdateListener(OnMetadataUpdateListener onMetaDataUpdateListener) {
        onMetaDataUpdateListeners.remove(onMetaDataUpdateListener);
    }

    public interface OnMetadataUpdateListener {
        void onMetaDataUpdated();
    }

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks = new SimpleActivityLifecycleCallbacks() {
        @Override
        public void onActivityStarted(Activity activity) {
            updateMetadata();
        }
    };

    /**
     * Enables debug logging.
     */
    public static void setDebugLoggingEnabled(boolean enabled) {
        Logger.setEnabled(enabled);
    }

    Config getConfig() {
        return config;
    }

    /**
     * Unique identifier, different over device installations
     */
    public String getClientId() {
        return userPreferences.getClientId();
    }

    public UserPreferences getUserPreferences() {
        return userPreferences;
    }

    public static Snabble getInstance() {
        return instance;
    }

    public interface SetupCompletionListener {
        void onReady();

        void onError(Error error);
    }

    public static class SnabbleException extends Exception {
        private final Error error;

        SnabbleException(Error error) {
            this.error = error;
        }

        public Error getError() {
            return error;
        }

        @Override
        public String toString() {
            return "SnabbleException{" +
                    "error=" + error +
                    '}';
        }
    }

    public enum Error {
        UNSPECIFIED_ERROR,
        CONFIG_PARAMETER_MISSING,
        CONNECTION_TIMEOUT,
        INVALID_METADATA_FORMAT,
        INTERNAL_STORAGE_FULL
    }

    public static class Config {
        /**
         * The endpoint url of the snabble backend. For example "snabble.io" for the Production environment.
         */
        public String endpointBaseUrl;

        /**
         * Relative path from the assets folder which points to a bundled file which contains the metadata
         * <p>
         * This file gets initially used to initialize the sdk before network requests are made,
         * or be able to use the sdk in the case of no network connection.
         * <p>
         * Optional. If no file is specified every time the sdk is initialized we wait for a network response
         * from the backend.
         * <p>
         * It is HIGHLY recommended to provide bundled metadata to allow the sdk to function
         * without having a network connection.
         */
        public String bundledMetadataAssetPath;

        /**
         * The project identifier, which is used in the communication with the backend.
         */
        public String appId;

        /**
         * The secret needed for Totp token generation
         */
        public String secret;
        /**
         * Optional. Used to override the versionName appended to the metadata url.
         * <p>
         * Defaults to the versionName in the app package.
         * <p>
         * Must be in the format %d.%d
         */
        public String versionName;

        /**
         * Optional SSLSocketFactory that gets used for HTTP requests.
         * <p>
         * Requires also x509TrustManager to be set.
         */
        public SSLSocketFactory sslSocketFactory = null;

        /**
         * Optional X509TrustManager that gets used for HTTP requests.
         * <p>
         * Requires also sslSocketFactory to be set.
         */
        public X509TrustManager x509TrustManager = null;

        /**
         * If set to true, creates an full text index to support searching in the product database
         * using findByName or searchByName.
         * <p>
         * Note that this increases setup time of the ProductDatabase, and it may not be
         * immediately available offline.
         */
        public boolean generateSearchIndex = false;
    }
}
