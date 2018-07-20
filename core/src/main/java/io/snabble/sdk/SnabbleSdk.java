package io.snabble.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.gson.JsonObject;

import org.apache.commons.lang3.LocaleUtils;

import java.io.File;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

public class SnabbleSdk {
    public static class Config {
        /**
         * The endpoint url of the snabble backend. For example "snabble.io" for the Production environment.
         */
        public String endpointBaseUrl;

        /**
         * The project identifier, which is used in the communication with the backend.
         */
        public String projectId;

        /**
         * The JWT based client token required for all requests to the backend.
         */
        public String clientToken;

        /**
         * Relative url to the metadata.
         * <p>
         * In the format: /api/{projectId}/metadata/app/{platform}/{version}
         */
        public String metadataUrl;

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
         * The name if the product database which will be used to store the database under
         * the default android database directory.
         * <p>
         * The resulting path is the path from {@link android.content.Context#getDatabasePath(String)}
         *
         * If set to null, only online lookup of products is available.
         *
         */
        public String productDbName;

        /**
         * Relative path from the assets folder which points to a bundled file which contains the products
         * as an sqlite3 database.
         * <p>
         * This file gets initially used to initialize the product database before network requests are made,
         * or be able to use the product database in the case of no network connection.
         * <p>
         * Optional. If no file is specified and no product database is already present, the sdk initialization
         * is delayed until the network request for the product db is successful.
         * <p>
         * It is HIGHLY recommended to provide a bundled database to allow the sdk to function
         * without having a network connection.
         */
        public String productDbBundledAssetPath;

        /**
         * This is the revision id of the bundled product database. This is used to prevent
         * extracting the bundled database when the stored database is already newer.
         * <p>
         * When the bundled database revision is newer then the stored database, the stored
         * database will be overwritten by the bundled database
         */
        public int productDbBundledRevisionId = -1;

        /**
         * The bundled major schema version.
         */
        public int productDbBundledSchemaVersionMajor = -1;

        /**
         * The bundled minor schema version.
         */
        public int productDbBundledSchemaVersionMinor = -1;

        /**
         * If set to true, allows the database to be downloaded even if no seed is provided.
         *
         * When set to false, calls to {@link ProductDatabase#update()} will still download
         * the database if its missing, allowing for the ability of database downloads after
         * sdk initialization
         */
        public boolean productDbDownloadIfMissing = true;

        /**
         * If set to true, creates an full text index to support searching in the product database
         * using findByName or searchByName.
         *
         * Note that this increases setup time of the SDK and it is highly recommended to use
         * the non-blocking initialization function.
         */
        public boolean generateSearchIndex = false;

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

        public boolean useGermanPrintPrefix = false;

        public String encodedCodesPrefix = null;
        public String encodedCodesSeperator = null;
        public String encodedCodesSuffix = null;
        public int encodedCodesMaxCodes = 100;
    }

    private static UserPreferences userPreferences;

    private String endpointBaseUrl;
    private String metadataUrl;
    private String projectId;

    private Application application;
    private OkHttpClient okHttpClient;

    private MetadataDownloader metadataDownloader;

    private List<OnMetadataUpdateListener> onMetaDataUpdateListeners = new CopyOnWriteArrayList<>();

    private ProductDatabase productDatabase;
    private Shop[] shops;
    private Checkout checkout;
    private ShoppingCartManager shoppingCartManager;
    private Events events;

    private File internalStorageDirectory;
    private Currency currency;
    private int currencyFractionDigits = -1;
    private Locale currencyLocale;

    private String loyaltyCardId;

    private String[] pricePrefixes = new String[0];
    private String[] weighPrefixes = new String[0];
    private String[] unitPrefixes = new String[0];

    private String encodedCodesPrefix = null;
    private String encodedCodesSeperator = null;
    private String encodedCodesSuffix = null;
    private int encodedCodesMaxCodes;

    private boolean useGermanPrintPrefix = false;

    private void init(final Application app,
                      final Config config,
                      final SetupCompletionListener setupCompletionListener) {
        application = app;

        createOkHttpClient(config.clientToken, config.sslSocketFactory, config.x509TrustManager);
        internalStorageDirectory = new File(app.getFilesDir(), "snabble/" + config.projectId + "/");
        //noinspection ResultOfMethodCallIgnored
        internalStorageDirectory.mkdirs();

        if (userPreferences == null) {
            userPreferences = new UserPreferences(application);
        }

        projectId = config.projectId;

        if (projectId == null || application == null) {
            setupCompletionListener.onError(Error.CONFIG_PARAMETER_MISSING);
            return;
        }

        if(config.endpointBaseUrl == null){
            endpointBaseUrl = "https://api.snabble.io";
        } else if (config.endpointBaseUrl.startsWith("http://")
                || config.endpointBaseUrl.startsWith("https://")) {
            endpointBaseUrl = config.endpointBaseUrl;
        } else {
            endpointBaseUrl = "https://" + config.endpointBaseUrl;
        }

        if(config.metadataUrl == null){
            ApplicationInfo applicationInfo = app.getApplicationInfo();
            int stringId = applicationInfo.labelRes;
            String label = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : app.getString(stringId);
            String version;

            try {
                PackageInfo pInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                version = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                version = "";
            }

            label = label.toLowerCase().replace(" ", "");
            version = version.toLowerCase().replace(" ", "");

            metadataUrl = "/" + config.projectId + "/metadata/app/android/" + label + "-" + version;
        } else {
            metadataUrl = config.metadataUrl;
        }

        metadataUrl = absoluteUrl(metadataUrl);
        metadataDownloader = new MetadataDownloader(this, config.bundledMetadataAssetPath);

        encodedCodesPrefix = config.encodedCodesPrefix != null ? config.encodedCodesPrefix : "";
        encodedCodesSeperator = config.encodedCodesSeperator != null ? config.encodedCodesSeperator : "\n";
        encodedCodesSuffix = config.encodedCodesSuffix != null ? config.encodedCodesSuffix : "";
        encodedCodesMaxCodes = config.encodedCodesMaxCodes;

        useGermanPrintPrefix = config.useGermanPrintPrefix;

        updateShops();

        if (config.bundledMetadataAssetPath != null) {
            setupSdk(config, setupCompletionListener);
        } else {
            metadataDownloader.loadAsync(new Downloader.Callback() {
                @Override
                protected void onDataLoaded(boolean wasStillValid) {
                    setupSdk(config, setupCompletionListener);
                }

                @Override
                protected void onError() {
                    if(metadataDownloader.hasData()){
                        setupSdk(config, setupCompletionListener);
                    } else {
                        setupError(setupCompletionListener, Error.CONNECTION_TIMEOUT);
                    }
                }
            });
        }
    }

    private void createOkHttpClient(String clientToken,
                                    SSLSocketFactory sslSocketFactory,
                                    X509TrustManager x509TrustManager) {
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

        if (clientToken != null) {
            builder.addInterceptor(new SnabbleAuthorizationInterceptor(this, clientToken));
        }

        builder.addInterceptor(new UserAgentInterceptor(application));

        if(sslSocketFactory != null && x509TrustManager != null) {
            builder.sslSocketFactory(sslSocketFactory, x509TrustManager);
        }

        okHttpClient = builder.build();
    }

    private SnabbleSdk() {

    }

    /**
     * Initializes the snabble SDK. Returns a {@link SnabbleSdk} context without requiring network access in
     * {@link SetupCompletionListener#onReady(SnabbleSdk)} when
     * {@link Config#bundledMetadataAssetPath} and {@link Config#productDbBundledAssetPath} is set.
     * <p>
     * Otherwise a network request will be made and after successfully downloading the metadata and product database,
     * {@link SetupCompletionListener#onReady(SnabbleSdk)} will be called.
     * <p>
     * If no network request could be made and no {@link Config#bundledMetadataAssetPath} is provided
     * {@link Error#CONNECTION_TIMEOUT} will be called.
     */
    public static void setup(Application app, Config config,
                             SetupCompletionListener setupCompletionListener) {
        final SnabbleSdk sdk = new SnabbleSdk();
        sdk.init(app, config, setupCompletionListener);
    }

    /**
     * The blocking version of {@link SnabbleSdk#setup(Application, Config, SetupCompletionListener)}
     * <p>
     * Blocks until every initialization is completed, that includes waiting for necessary
     * network calls if bundled data is not provided.
     * <p>
     * If all needed bundled data is provided (See {@link Config}), initialization requires
     * no network calls and returns after initialization of the product database.
     *
     * @throws SnabbleException If an error occurs while initializing the sdk.
     */
    public static SnabbleSdk setupBlocking(Application app, Config config) throws SnabbleException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final SnabbleSdk[] snabbleSdk = new SnabbleSdk[1];
        final Error[] snabbleError = new Error[1];

        setup(app, config, new SetupCompletionListener() {
            @Override
            public void onReady(SnabbleSdk sdk) {
                snabbleSdk[0] = sdk;
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

        return snabbleSdk[0];
    }

    public boolean isUsingGermanPrintPrefix() {
        return useGermanPrintPrefix;
    }

    private void updateShops() {
        String shopsJson = metadataDownloader.getExtras().get("shops");
        if (shopsJson != null) {
            shops = Shop.fromJson(shopsJson);
        }

        if (shops == null) {
            shops = new Shop[0];
        }
    }

    private void setupSdk(Config config, final SetupCompletionListener setupCompletionListener) {
        readMetadata();

        ProductDatabase.Config dbConfig = new ProductDatabase.Config();
        dbConfig.bundledAssetPath = config.productDbBundledAssetPath;
        dbConfig.bundledRevisionId = config.productDbBundledRevisionId;
        dbConfig.bundledSchemaVersionMajor = config.productDbBundledSchemaVersionMajor;
        dbConfig.bundledSchemaVersionMinor = config.productDbBundledSchemaVersionMinor;
        dbConfig.autoUpdateIfMissing = config.productDbDownloadIfMissing;
        dbConfig.generateSearchIndex = config.generateSearchIndex;

        productDatabase = new ProductDatabase(this,
                config.productDbName,
                dbConfig,
                new ProductDatabase.ProductDatabaseReadyListener() {
                    @Override
                    public void onReady(ProductDatabase productDatabase) {
                        SnabbleSdk.this.productDatabase = productDatabase;
                        shoppingCartManager = new ShoppingCartManager(SnabbleSdk.this);
                        checkout = new Checkout(SnabbleSdk.this);
                        events = new Events(SnabbleSdk.this);
                        setupOk(setupCompletionListener);
                    }

                    @Override
                    public void onError(ProductDatabase.Error error) {
                        switch (error) {
                            case CONNECTION_TIMEOUT:
                                setupError(setupCompletionListener, Error.CONNECTION_TIMEOUT);
                                break;
                            case INTERNAL_STORAGE_FULL:
                                setupError(setupCompletionListener, Error.INTERNAL_STORAGE_FULL);
                                break;
                            default:
                                setupError(setupCompletionListener, Error.UNSPECIFIED_ERROR);
                                break;
                        }
                    }
                });
    }

    private void readMetadata() {
        updateShops();

        Map<String, String> urls = metadataDownloader.getUrls();
        if (urls == null || urls.get("appdb") == null) {
            Logger.e("Metadata does not contain url for product database updates. No product database updates possible");
        }

        JsonObject projectSettings = metadataDownloader.getProject();

        currency = Currency.getInstance(JsonUtils.getStringOpt(projectSettings, "currency", "EUR"));

        String locale = JsonUtils.getStringOpt(projectSettings, "locale", "de_DE");

        try {
            currencyLocale = LocaleUtils.toLocale(locale);
        } catch (IllegalArgumentException e){
            currencyLocale = Locale.getDefault();
        }

        if(currencyLocale == null){
            currencyLocale = Locale.getDefault();
        }

        currencyFractionDigits = JsonUtils.getIntOpt(projectSettings, "decimalDigits", 2);

        weighPrefixes = JsonUtils.getStringArrayOpt(projectSettings, "weighPrefixes", new String[0]);
        pricePrefixes = JsonUtils.getStringArrayOpt(projectSettings, "pricePrefixes", new String[0]);
        unitPrefixes = JsonUtils.getStringArrayOpt(projectSettings, "unitPrefixes", new String[0]);
    }

    private void setupOk(final SetupCompletionListener setupCompletionListener) {
        if (setupCompletionListener != null) {
            setupCompletionListener.onReady(SnabbleSdk.this);
        }

        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    private void setupError(final SetupCompletionListener setupCompletionListener,
                            final Error error) {
        if (setupCompletionListener != null) {
            setupCompletionListener.onError(error);
        }
    }

    File getInternalStorageDirectory() {
        return internalStorageDirectory;
    }

    public String getEndpointBaseUrl() {
        return endpointBaseUrl;
    }

    public String getProjectId() {
        return projectId;
    }

    OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    Application getApplication() {
        return application;
    }

    String getEventsUrl() {
        return metadataDownloader.getUrls().get("appEvents");
    }

    String getAppDbUrl() {
        return metadataDownloader.getUrls().get("appdb");
    }

    String getCheckoutUrl() {
        return metadataDownloader.getUrls().get("checkoutInfo");
    }

    String getMetadataUrl() {
        if (loyaltyCardId != null) {
            return metadataUrl + "?loyaltyCard=" + loyaltyCardId;
        } else {
            return metadataUrl;
        }
    }

    String getProductBySkuUrl() {
        return metadataDownloader.getUrls().get("productBySku");
    }

    String getProductByCodeUrl() {
        return metadataDownloader.getUrls().get("productByCode");
    }

    String getProductByWeighItemIdUrl() {
        return metadataDownloader.getUrls().get("productByWeighItemId");
    }

    public String[] getPricePrefixes() {
        return pricePrefixes;
    }

    public String[] getWeighPrefixes() {
        return weighPrefixes;
    }

    public String[] getUnitPrefixes() {
        return unitPrefixes;
    }

    public String getEncodedCodesPrefix() {
        return encodedCodesPrefix;
    }

    public String getEncodedCodesSeperator() {
        return encodedCodesSeperator;
    }

    public String getEncodedCodesSuffix() {
        return encodedCodesSuffix;
    }

    public int getEncodedCodesMaxCodes() {
        return encodedCodesMaxCodes;
    }

    String absoluteUrl(String url) {
        if (url.startsWith("http")) {
            return url;
        } else {
            return getEndpointBaseUrl() + url;
        }
    }

    /**
     * Enables debug logging.
     */
    public static void setDebugLoggingEnabled(boolean enabled) {
        Logger.setEnabled(enabled);
    }

    /**
     * Returns the {@link ProductDatabase}.
     */
    public ProductDatabase getProductDatabase() {
        return productDatabase;
    }

    /**
     * A key-value map containing urls provided by the metadata.
     * All urls are absolute, even if the original metadata contained relative urls.
     */
    public Map<String, String> getUrls() {
        return metadataDownloader.getUrls();
    }

    /**
     * A key-value map containing extra data in the metadata.
     */
    public Map<String, String> getExtras() {
        return metadataDownloader.getExtras();
    }

    /**
     * A json object that contains various flags provided by the metadata service.
     * <p>
     * May also contain additional flags specific to projects.
     */
    public JsonObject getFlags() {
        return metadataDownloader.getMetadata();
    }

    /**
     * A json object that contains various flags provided by the metadata service.
     * <p>
     * May also contain additional flags specific to projects.
     */
    public JsonObject getProjectSettings() {
        return metadataDownloader.getProject();
    }

    /**
     * @return The available shops. Empty if no shops are defined.
     */
    public Shop[] getShops() {
        return shops;
    }

    public ShoppingCart getShoppingCart() {
        return shoppingCartManager.getShoppingCart();
    }

    public Currency getCurrency() {
        return currency;
    }

    public int getCurrencyFractionDigits() {
        if (currencyFractionDigits == -1) {
            return currency.getDefaultFractionDigits();
        } else {
            return currencyFractionDigits;
        }
    }

    public Locale getCurrencyLocale() {
        return currencyLocale;
    }

    public RoundingMode getRoundingMode() {
        return metadataDownloader.getRoundingMode();
    }

    public boolean isVerifyingInternalEanChecksum() {
        return metadataDownloader.isVerifyingInternalEanChecksum();
    }

    public Checkout getCheckout() {
        return checkout;
    }

    Events getEvents() {
        return events;
    }

    /**
     * Sets the customer loyalty card number for user identification with the backend.
     * <p>
     * Setting this causes the metadata to be updated and it may contain different
     * information then before. (e.g. Feature-toggle flags, different URL's)
     */
    public void setLoyaltyCardId(String loyaltyCardId) {
        this.loyaltyCardId = loyaltyCardId;
        updateMetadata();
    }

    public String getLoyaltyCardId() {
        return loyaltyCardId;
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

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks = new SimpleActivityLifecycleCallbacks() {
        @Override
        public void onActivityStarted(Activity activity) {
            updateMetadata();
            getShoppingCart().checkForTimeout();
        }
    };

    public static String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Unique identifier, different over device installations
     */
    public static String getClientId(){
        return userPreferences.getClientId();
    }

    public static UserPreferences getUserPreferences() {
        return userPreferences;
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

    public interface SetupCompletionListener {
        void onReady(SnabbleSdk sdk);

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
}
