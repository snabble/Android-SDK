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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import io.snabble.sdk.utils.Downloader;
import io.snabble.sdk.utils.JsonUtils;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class Snabble {
    private static Snabble instance = new Snabble();

    private List<Project> projects;
    private OkHttpClient okHttpClient;
    private Application application;
    private MetadataDownloader metadataDownloader;
    private UserPreferences userPreferences;
    private File internalStorageDirectory;
    private String metadataUrl;
    private Config config;
    private List<OnMetadataUpdateListener> onMetaDataUpdateListeners = new CopyOnWriteArrayList<>();

    private Snabble() {

    }

    public void setup(Application app, Config config, final SetupCompletionListener setupCompletionListener) {
        this.application = app;
        this.config = config;
        createOkHttpClient();

        this.userPreferences = new UserPreferences(app);

        internalStorageDirectory = new File(application.getFilesDir(), "snabble/" + config.appId + "/");
        //noinspection ResultOfMethodCallIgnored
        internalStorageDirectory.mkdirs();

        projects = Collections.unmodifiableList(new ArrayList<Project>());

        if (config.appId == null || application == null) {
            setupCompletionListener.onError(Error.CONFIG_PARAMETER_MISSING);
            return;
        }

        if(config.endpointBaseUrl == null){
            config.endpointBaseUrl = "https://api.snabble.io";
        } else if (!config.endpointBaseUrl.startsWith("http://") && !config.endpointBaseUrl.startsWith("https://")) {
            config.endpointBaseUrl = "https://" + config.endpointBaseUrl;
        }

        String version;

        try {
            PackageInfo pInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "0.1";
        }

        version = version.toLowerCase().replace(" ", "");

        metadataUrl = absoluteUrl("/metadata/app/" + config.appId + "/android/" + version);

        this.metadataDownloader = new MetadataDownloader(config.bundledMetadataAssetPath);

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
                    if(metadataDownloader.hasData()){
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

    // TODO alternative name
    public boolean shouldKillApp() {
        JsonObject jsonObject = metadataDownloader.getJsonObject();
        return jsonObject.has("metadata") && JsonUtils.getBooleanOpt(jsonObject, "metadata", false);
    }

    private void readMetadata() {
        JsonObject jsonObject = metadataDownloader.getJsonObject();
        if(jsonObject.has("projects")) {
            JsonArray jsonArray = jsonObject.get("projects").getAsJsonArray();
            List<Project> newProjects = new ArrayList<>();

            for (int i=0; i<jsonArray.size(); i++) {
                JsonObject jsonProject = jsonArray.get(i).getAsJsonObject();
                try {
                    Project project = new Project(jsonProject);
                    newProjects.add(project);
                } catch (IllegalArgumentException e) {
                    // malformed project, do nothing
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

    OkHttpClient getOkHttpClient() {
        return okHttpClient;
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
     * If all needed bundled data is provided (See {@link Project.Config}), initialization requires
     * no network calls and returns after initialization of the product database.
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

    private void createOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.cache(new Cache(application.getCacheDir(), 10485760)); //10 MB

        builder.retryOnConnectionFailure(true);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        Logger.i(message);
                    }
                });
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        builder.addInterceptor(logging);

        if (config.clientToken != null) {
            builder.addInterceptor(new SnabbleAuthorizationInterceptor(config.clientToken));
        }

        builder.addInterceptor(new UserAgentInterceptor(application));

        if(config.sslSocketFactory != null && config.x509TrustManager != null) {
            builder.sslSocketFactory(config.sslSocketFactory, config.x509TrustManager);
        }

        okHttpClient = builder.build();
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

    /**
     * Unique identifier, different over device installations
     */
    public String getClientId(){
        return userPreferences.getClientId();
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
         * The JWT based client token required for all requests to the backend.
         */
        public String clientToken;

        /**
         * The endpoint url of the snabble backend. For example "snabble.io" for the Production environment.
         */
        public String endpointBaseUrl;

        /**
         * Relative path from the assets folder which points to a bundled file which contains the metadata
         * from the metadataUrl specified before.
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
         * Optional SSLSocketFactory that gets used for HTTP requests.
         *
         * Requires also x509TrustManager to be set.
         */
        public SSLSocketFactory sslSocketFactory = null;

        /**
         * Optional X509TrustManager that gets used for HTTP requests.
         *
         * Requires also sslSocketFactory to be set.
         */
        public X509TrustManager x509TrustManager = null;

        /**
         * If set to true, creates an full text index to support searching in the product database
         * using findByName or searchByName.
         *
         * Note that this increases setup time of the SDK and it is highly recommended to use
         * the non-blocking initialization function.
         */
        public boolean generateSearchIndex = false;
    }
}
