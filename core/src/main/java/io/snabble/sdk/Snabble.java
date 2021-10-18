package io.snabble.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import io.snabble.sdk.checkin.CheckInLocationManager;
import io.snabble.sdk.checkin.CheckInManager;
import io.snabble.sdk.auth.AppUser;
import io.snabble.sdk.auth.Token;
import io.snabble.sdk.auth.TokenRegistry;
import io.snabble.sdk.payment.PaymentCredentialsStore;
import io.snabble.sdk.utils.Downloader;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.JsonUtils;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import okhttp3.OkHttpClient;

public class Snabble {
    private static Snabble instance = new Snabble();

    private Map<String, Brand> brands;
    private List<Project> projects;
    private TokenRegistry tokenRegistry;
    private Receipts receipts;
    private Users users;
    private Application application;
    private MetadataDownloader metadataDownloader;
    private UserPreferences userPreferences;
    private PaymentCredentialsStore paymentCredentialsStore;
    private CheckInLocationManager checkInLocationManager;
    private CheckInManager checkInManager;
    private File internalStorageDirectory;
    private String metadataUrl;
    private Config config;
    private List<OnMetadataUpdateListener> onMetaDataUpdateListeners = new CopyOnWriteArrayList<>();
    private String versionName;
    private Environment environment;
    private TermsOfService termsOfService;
    private List<X509Certificate> paymentCertificates;
    private String receiptsUrl;
    private String usersUrl;
    private String consentUrl;
    private String telecashSecretUrl;
    private String telecashPreAuthUrl;
    private String paydirektAuthUrl;
    private String createAppUserUrl;
    private OkHttpClient okHttpClient;
    private WeakReference<Activity> currentActivity;

    private Snabble() {

    }

    public void setup(Application app, Config config, final SetupCompletionListener setupCompletionListener) {
        this.application = app;
        this.config = config;

        Logger.setErrorEventHandler((message, args) -> Events.logErrorEvent(null, message, args));
        Logger.setLogEventHandler((message, args) -> Events.logErrorEvent(null, message, args));

        if (config.appId == null || config.secret == null) {
            setupCompletionListener.onError(Error.CONFIG_PARAMETER_MISSING);
            return;
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

        internalStorageDirectory = new File(application.getFilesDir(), "snabble/" + config.appId + "/");
        //noinspection ResultOfMethodCallIgnored
        internalStorageDirectory.mkdirs();

        okHttpClient = OkHttpClientFactory.createOkHttpClient(app);
        userPreferences = new UserPreferences(app);
        tokenRegistry = new TokenRegistry(okHttpClient, userPreferences, config.appId, config.secret);
        receipts = new Receipts();
        users = new Users(userPreferences);

        brands = Collections.unmodifiableMap(new HashMap<>());
        projects = Collections.unmodifiableList(new ArrayList<>());

        if (config.endpointBaseUrl == null) {
            config.endpointBaseUrl = Environment.PRODUCTION.getBaseUrl();
        } else if (!config.endpointBaseUrl.startsWith("http://") && !config.endpointBaseUrl.startsWith("https://")) {
            config.endpointBaseUrl = "https://" + config.endpointBaseUrl;
        }

        environment = Environment.getEnvironmentByUrl(config.endpointBaseUrl);
        metadataUrl = absoluteUrl("/metadata/app/" + config.appId + "/android/" + version);
        paymentCredentialsStore = new PaymentCredentialsStore();
        checkInLocationManager = new CheckInLocationManager(application);
        checkInManager = new CheckInManager(this, checkInLocationManager);

        this.metadataDownloader = new MetadataDownloader(okHttpClient, config.bundledMetadataAssetPath);

        if (config.bundledMetadataAssetPath != null) {
            readMetadata();
            setupCompletionListener.onReady();

            if (config.loadActiveShops) {
                loadActiveShops();
            }
        } else {
            metadataDownloader.loadAsync(new Downloader.Callback() {
                @Override
                protected void onDataLoaded(boolean wasStillValid) {
                    readMetadata();

                    AppUser appUser = userPreferences.getAppUser();
                    if (appUser == null && projects.size() > 0) {
                        Token token = tokenRegistry.getToken(projects.get(0));
                        if (token == null) {
                            setupCompletionListener.onError(Error.CONNECTION_TIMEOUT);
                            return;
                        }
                    }

                    setupCompletionListener.onReady();

                    if (config.loadActiveShops) {
                        loadActiveShops();
                    }
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
        registerNetworkCallback(app);
    }

    private void registerNetworkCallback(Application app) {
        ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);

        cm.registerNetworkCallback(new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build(),
                networkCallback);
    }

    public String getVersionName() {
        return versionName;
    }

    /**
     * Returns true when the SDK is not compatible with the backend anymore and the app should
     * notify the user that it will not function anymore.
     */
    public boolean isOutdatedSDK() {
        JsonObject jsonObject = getAdditionalMetadata();
        if (jsonObject != null) {
            return JsonUtils.getBooleanOpt(jsonObject, "kill", false);
        }

        return false;
    }

    /** Returns additional metadata that may be provided for apps unrelated to the SDK **/
    public JsonObject getAdditionalMetadata() {
        JsonObject jsonObject = metadataDownloader.getJsonObject();

        JsonElement jsonElement = jsonObject.get("metadata");
        if (jsonElement != null) {
            return jsonElement.getAsJsonObject();
        }

        return null;
    }

    private synchronized void readMetadata() {
        JsonObject jsonObject = metadataDownloader.getJsonObject();
        if (jsonObject != null) {
            createAppUserUrl = getUrl(jsonObject, "createAppUser");
            telecashSecretUrl = getUrl(jsonObject, "telecashSecret");
            telecashPreAuthUrl = getUrl(jsonObject, "telecashPreauth");
            paydirektAuthUrl = getUrl(jsonObject, "paydirektCustomerAuthorization");

            if (jsonObject.has("brands")) {
                parseBrands(jsonObject);
            }

            if (jsonObject.has("projects")) {
                parseProjects(jsonObject);
            }

            if (jsonObject.has("gatewayCertificates")) {
                parsePaymentCertificates(jsonObject);
            }

            receiptsUrl = getUrl(jsonObject, "appUserOrders");
            usersUrl = getUrl(jsonObject, "appUser");
            consentUrl = getUrl(jsonObject, "consents");

            if (jsonObject.has("terms")) {
                termsOfService = GsonHolder.get().fromJson(jsonObject.get("terms"), TermsOfService.class);
            }
        }

        paymentCredentialsStore.init(application, environment);
        users.postPendingConsents();
    }

    private String getUrl(JsonObject jsonObject, String urlName) {
        try {
            return absoluteUrl(jsonObject.get("links").getAsJsonObject()
                    .get(urlName).getAsJsonObject()
                    .get("href").getAsString());
        } catch (Exception e) {
            return null;
        }
    }

    private void parseBrands(JsonObject jsonObject) {
       Brand[] jsonBrands = GsonHolder.get().fromJson(jsonObject.get("brands"), Brand[].class);
       HashMap<String, Brand> map = new HashMap<>();
       for (Brand brand : jsonBrands) {
           map.put(brand.getId(), brand);
       }
       brands = Collections.unmodifiableMap(map);
    }

    private void parseProjects(JsonObject jsonObject) {
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
                        Logger.d(e.getMessage());
                        // malformed project, do nothing
                    }
                }
            }
        }

        projects = Collections.unmodifiableList(newProjects);
    }

    private void parsePaymentCertificates(JsonObject jsonObject) {
        List<X509Certificate> certificates = new ArrayList<>();

        JsonArray certs = jsonObject.get("gatewayCertificates").getAsJsonArray();
        for (int i=0; i<certs.size(); i++) {
            JsonElement jsonElement = certs.get(i);
            if (jsonElement.isJsonObject()) {
                JsonObject cert = jsonElement.getAsJsonObject();
                JsonElement value = cert.get("value");
                if (value != null) {
                    byte[] bytes = Base64.decode(value.getAsString(), Base64.DEFAULT);
                    InputStream is = new ByteArrayInputStream(bytes);

                    try {
                        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                        X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(is);
                        certificates.add(certificate);
                    } catch (CertificateException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        paymentCertificates = Collections.unmodifiableList(certificates);
    }

    public String absoluteUrl(String url) {
        if (url == null) {
            return null;
        }

        if (url.startsWith("http")) {
            return url;
        } else {
            return getEndpointBaseUrl() + url;
        }
    }

    public String getEndpointBaseUrl() {
        return config.endpointBaseUrl;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public String getReceiptsUrl() {
        AppUser appUser = userPreferences.getAppUser();
        if (appUser != null && receiptsUrl != null) {
            return receiptsUrl.replace("{appUserID}", userPreferences.getAppUser().id);
        } else {
            return null;
        }
    }

    public String getTelecashSecretUrl() {
        return telecashSecretUrl;
    }

    public String getTelecashPreAuthUrl() {
        return telecashPreAuthUrl;
    }

    public String getPaydirektAuthUrl() {
        return paydirektAuthUrl;
    }

    public String getCreateAppUserUrl() {
        return createAppUserUrl;
    }

    public String getUsersUrl() {
        return usersUrl;
    }

    public String getConsentUrl() {
        return consentUrl;
    }

    public File getInternalStorageDirectory() {
        return internalStorageDirectory;
    }

    public Application getApplication() {
        return application;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public TokenRegistry getTokenRegistry() {
        return tokenRegistry;
    }

    public Receipts getReceipts() {
        return receipts;
    }

    public Users getUsers() {
        return users;
    }

    public TermsOfService getTermsOfService() {
        return termsOfService;
    }

    public CheckInLocationManager getCheckInLocationManager() {
        return checkInLocationManager;
    }

    public CheckInManager getCheckInManager() {
        return checkInManager;
    }

    public Map<String, Brand> getBrands() {
        return brands;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public Project getProjectById(String projectId) {
        for (Project project : projects) {
            if (project.getId().equals(projectId)) {
                return project;
            }
        }

        return null;
    }

    public List<X509Certificate> getPaymentSigningCertificates() {
        return Collections.unmodifiableList(paymentCertificates);
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
                    notifyMetadataUpdated();
                }

                if (config.loadActiveShops) {
                    loadActiveShops();
                }
            }
        });
    }

    private void loadActiveShops() {
        for (Project project : projects) {
            project.loadActiveShops(this::notifyMetadataUpdated);
        }
    }

    private void notifyMetadataUpdated() {
        for (OnMetadataUpdateListener listener : onMetaDataUpdateListeners) {
            listener.onMetaDataUpdated();
        }
    }

    private void checkCartTimeouts() {
        for (Project project : projects) {
            project.getShoppingCart().checkForTimeout();
        }
    }

    private void processPendingCheckouts() {
        for (Project project : projects) {
            project.getCheckout().processPendingCheckouts();
        }
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

    /**
     * Sets the current activity context.
     *
     * This is an internal method and should not be called.
     */
    public void _setCurrentActivity(Activity activity) {
        if (currentActivity != null) {
            currentActivity.clear();
            currentActivity = null;
        }

        currentActivity = new WeakReference<>(activity);
    }

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks = new SimpleActivityLifecycleCallbacks() {

        @Override
        public void onActivityStarted(Activity activity) {
            if (currentActivity != null) {
                currentActivity.clear();
                currentActivity = null;
            }

            currentActivity = new WeakReference<>(activity);
            updateMetadata();
            checkCartTimeouts();
            processPendingCheckouts();
        }

        @Override
        public void onActivityStopped(Activity activity) {
            if (currentActivity != null) {
                if (currentActivity.get() == activity) {
                    currentActivity.clear();
                    currentActivity = null;
                }
            }
        }
    };


    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            onConnectionStateChanged(true);
        }

        @Override
        public void onLost(@NonNull Network network) {
            onConnectionStateChanged(false);
        }

        @Override
        public void onUnavailable() {
            onConnectionStateChanged(false);
        }
    };

    private void onConnectionStateChanged(boolean isConnected) {
        if (isConnected) {
            processPendingCheckouts();
        }

        for (Project project : projects) {
            project.getShoppingCart().updatePrices(false);
        }
    }

    /**
     * Enables debug logging.
     */
    public static void setDebugLoggingEnabled(boolean enabled) {
        Logger.setEnabled(enabled);
    }

    public Config getConfig() {
        return config;
    }

    /**
     * Unique identifier, different over device installations
     */
    public String getClientId() {
        return userPreferences.getClientId();
    }

    public Environment getEnvironment() {
        return environment;
    }

    public UserPreferences getUserPreferences() {
        return userPreferences;
    }

    public PaymentCredentialsStore getPaymentCredentialsStore() {
        return paymentCredentialsStore;
    }

    public static Snabble getInstance() {
        return instance;
    }

    public Activity getCurrentActivity() {
        if (currentActivity != null) {
            return currentActivity.get();
        }

        return null;
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
         *
         * If null points to the Production Environment
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
         * Optional SSLSocketFactory that gets used for HTTPS requests.
         * <p>
         * Requires also x509TrustManager to be set.
         */
        public SSLSocketFactory sslSocketFactory = null;

        /**
         * Optional X509TrustManager that gets used for HTTPS requests.
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
        public boolean generateSearchIndex;

        /**
         * The time that the database is allowed to be out of date. After the specified time in
         * milliseconds the database only uses online requests for asynchronous requests.
         *
         * Successfully calling {@link ProductDatabase#update()} resets the timer.
         *
         * The time is specified in milliseconds.
         *
         * The default value is 1 hour.
         */
        public long maxProductDatabaseAge = TimeUnit.HOURS.toMillis(1);

        /**
         * The time that the shopping cart is allowed to be alive after the last modification.
         *
         * The time is specified in milliseconds.
         *
         * The default value is 4 hours.
         */
        public long maxShoppingCartAge = TimeUnit.HOURS.toMillis(4);

        /** If set to true, disables certificate pinning **/
        public boolean disableCertificatePinning;

        /** SQL queries that will get executed in order on the product database **/
        public String[] initialSQL = null;

        /** Vibrate while adding a product to the cart, by default false */
        public boolean vibrateToConfirmCartFilled = false;

        /** Set to true, to load shops that are marked as pre launch
         *  and are not part of the original metadata in the backend
         *  (for example for testing shops in production before a go-live) **/
        public boolean loadActiveShops = false;

        /**
         * When set to true does disable the automatic check in and polling of location.
         *
         * When disabled you are either required to manually use
         * CheckInManager.setShop or CheckInManager.startUpdating()
         */
        public boolean disableAutomaticCheckin = false;
    }
}
