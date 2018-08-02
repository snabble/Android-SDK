package io.snabble.sdk;

import android.app.Activity;
import android.app.Application;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.LocaleUtils;

import java.math.RoundingMode;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.snabble.sdk.utils.JsonUtils;
import io.snabble.sdk.utils.Logger;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;

public class Project {
    public static class Config {
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
    }

    private String id;

    private ProductDatabase productDatabase;
    private Shop[] shops;
    private Checkout checkout;
    private ShoppingCartManager shoppingCartManager;
    private Events events;

    private Currency currency;
    private int currencyFractionDigits;
    private Locale currencyLocale;

    private String loyaltyCardId;

    private String[] pricePrefixes;
    private String[] weighPrefixes;
    private String[] unitPrefixes;

    private String encodedCodesPrefix;
    private String encodedCodesSeparator;
    private String encodedCodesSuffix;
    private int encodedCodesMaxCodes;

    private boolean useGermanPrintPrefix;

    private RoundingMode roundingMode;
    private boolean verifyInternalEanChecksum;

    private Map<String, String> urls;

    Project(JsonObject jsonObject) throws IllegalArgumentException {
        Snabble snabble = Snabble.getInstance();

        Map<String, String> urls = new HashMap<>();

        if (jsonObject.has("id")) {
            id = jsonObject.get("id").getAsString();
        } else {
            throw new IllegalArgumentException("Project has no id");
        }

        JsonObject links = jsonObject.get("links").getAsJsonObject();
        Set<String> linkKeys = links.keySet();
        for (String k : linkKeys) {
            urls.put(k, snabble.absoluteUrl(links.get(k).getAsJsonObject().get("href").getAsString()));
        }

        this.urls = Collections.unmodifiableMap(urls);

        this.roundingMode = parseRoundingMode(jsonObject.get("roundingMode"));
        this.verifyInternalEanChecksum = JsonUtils.getBooleanOpt(jsonObject, "verifyInternalEanChecksum", true);

        currency = Currency.getInstance(JsonUtils.getStringOpt(jsonObject, "currency", "EUR"));

        String locale = JsonUtils.getStringOpt(jsonObject, "locale", "de_DE");

        try {
            currencyLocale = LocaleUtils.toLocale(locale);
        } catch (IllegalArgumentException e){
            currencyLocale = Locale.getDefault();
        }

        if(currencyLocale == null){
            currencyLocale = Locale.getDefault();
        }

        currencyFractionDigits = JsonUtils.getIntOpt(jsonObject, "decimalDigits", 2);

        weighPrefixes = JsonUtils.getStringArrayOpt(jsonObject, "weighPrefixes", new String[0]);
        pricePrefixes = JsonUtils.getStringArrayOpt(jsonObject, "pricePrefixes", new String[0]);
        unitPrefixes = JsonUtils.getStringArrayOpt(jsonObject, "unitPrefixes", new String[0]);

        // TODO remove temporary flags when contained in the metadata json
        if(id.equals("edeka-paschmann-84f311")) {
            encodedCodesPrefix = "XE";
            encodedCodesSeparator = "XE";
            encodedCodesSuffix = "XZ";
            encodedCodesMaxCodes = 30;
        } else {
            encodedCodesPrefix = "";
            encodedCodesSeparator = "\n";
            encodedCodesSuffix = "";
            encodedCodesMaxCodes = 100;
        }

        if(id.equals("demo") || id.equals("edeka-paschmann-84f311")) {
            useGermanPrintPrefix = true;
        }

        if (jsonObject.has("shops")) {
            shops = Shop.fromJson(jsonObject.get("shops"));
        }

        if (shops == null) {
            shops = new Shop[0];
        }

        ProductDatabase.Config dbConfig = new ProductDatabase.Config();
//        dbConfig.bundledAssetPath = config.productDbBundledAssetPath;
//        dbConfig.bundledRevisionId = config.productDbBundledRevisionId;
//        dbConfig.bundledSchemaVersionMajor = config.productDbBundledSchemaVersionMajor;
//        dbConfig.bundledSchemaVersionMinor = config.productDbBundledSchemaVersionMinor;
//        dbConfig.autoUpdateIfMissing = config.productDbDownloadIfMissing;
//        dbConfig.generateSearchIndex = config.generateSearchIndex;

        productDatabase = new ProductDatabase(this, id + ".sqlite3", dbConfig);

        shoppingCartManager = new ShoppingCartManager(this);
        checkout = new Checkout(this);
        events = new Events(this);

        Application app = Snabble.getInstance().getApplication();
        app.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    private RoundingMode parseRoundingMode(JsonElement jsonElement){
        if(jsonElement != null){
            String roundingMode = jsonElement.getAsString();
            if(roundingMode != null){
                switch(roundingMode){
                    case "up":
                        return RoundingMode.UP;
                    case "down":
                        return RoundingMode.DOWN;
                    case "commercial":
                        return RoundingMode.HALF_UP;
                }
            }
        }

        return RoundingMode.HALF_UP;
    }

    public boolean isUsingGermanPrintPrefix() {
        return useGermanPrintPrefix;
    }

    public String getId() {
        return id;
    }

    String getEventsUrl() {
        return urls.get("appEvents");
    }

    String getAppDbUrl() {
        return urls.get("appdb");
    }

    String getCheckoutUrl() {
        return urls.get("checkoutInfo");
    }

    String getProductBySkuUrl() {
        return urls.get("productBySku");
    }

    String getProductByCodeUrl() {
        return urls.get("productByCode");
    }

    String getBundlesOfProductUrl() {
        return urls.get("bundlesForSku");
    }

    String getProductByWeighItemIdUrl() {
        return urls.get("productByWeighItemId");
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

    public String getEncodedCodesSeparator() {
        return encodedCodesSeparator;
    }

    public String getEncodedCodesSuffix() {
        return encodedCodesSuffix;
    }

    public int getEncodedCodesMaxCodes() {
        return encodedCodesMaxCodes;
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
        return urls;
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
        return roundingMode;
    }

    public boolean isVerifyingInternalEanChecksum() {
        return verifyInternalEanChecksum;
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
    }

    public String getLoyaltyCardId() {
        return loyaltyCardId;
    }

    private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks = new SimpleActivityLifecycleCallbacks() {
        @Override
        public void onActivityStarted(Activity activity) {
            getShoppingCart().checkForTimeout();
        }
    };

}
