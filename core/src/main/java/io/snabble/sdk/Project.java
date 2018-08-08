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
import okhttp3.OkHttpClient;

public class Project {

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
    private boolean isCheckoutAvailable;

    private String encodedCodesPrefix;
    private String encodedCodesSeparator;
    private String encodedCodesSuffix;
    private int encodedCodesMaxCodes;

    private boolean useGermanPrintPrefix;

    private RoundingMode roundingMode;
    private boolean verifyInternalEanChecksum;

    private Map<String, String> urls;

    private OkHttpClient okHttpClient;

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

        isCheckoutAvailable = JsonUtils.getBooleanOpt(jsonObject, "enableCheckout", true);

        if(jsonObject.has("encodedCodes")) {
            JsonElement encodedCodes = jsonObject.get("encodedCodes");
            if(!encodedCodes.isJsonNull()) {
                JsonObject object = encodedCodes.getAsJsonObject();

                encodedCodesPrefix = JsonUtils.getStringOpt(object, "prefix", "");
                encodedCodesSeparator = JsonUtils.getStringOpt(object, "separator", "\n");
                encodedCodesSuffix = JsonUtils.getStringOpt(object, "suffix", "");
                encodedCodesMaxCodes = JsonUtils.getIntOpt(object, "maxCodes", 100);
            }
        } else {
            encodedCodesPrefix = "";
            encodedCodesSeparator = "\n";
            encodedCodesSuffix = "";
            encodedCodesMaxCodes = 100;
        }

        useGermanPrintPrefix = JsonUtils.getBooleanOpt(jsonObject, "useGermanPrintPrefix", false);

        if (jsonObject.has("shops")) {
            shops = Shop.fromJson(jsonObject.get("shops"));
        }

        if (shops == null) {
            shops = new Shop[0];
        }

        okHttpClient = OkHttpClientFactory.createOkHttpClient(Snabble.getInstance().getApplication(), this);

        boolean generateSearchIndex = Snabble.getInstance().getConfig().generateSearchIndex;
        productDatabase = new ProductDatabase(this, id + ".sqlite3", generateSearchIndex);

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

    public String getTokensUrl() {
        return urls.get("tokens");
    }

    public String getEventsUrl() {
        return urls.get("appEvents");
    }

    public String getAppDbUrl() {
        return urls.get("appdb");
    }

    public String getCheckoutUrl() {
        return urls.get("checkoutInfo");
    }

    public String getProductBySkuUrl() {
        return urls.get("productBySku");
    }

    public String getProductByCodeUrl() {
        return urls.get("productByCode");
    }

    public String getBundlesOfProductUrl() {
        return urls.get("bundlesForSku");
    }

    public String getProductByWeighItemIdUrl() {
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

    public boolean isCheckoutAvailable() {
        return isCheckoutAvailable;
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

    OkHttpClient getOkHttpClient() {
        return okHttpClient;
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
