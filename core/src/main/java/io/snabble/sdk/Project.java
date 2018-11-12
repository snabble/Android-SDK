package io.snabble.sdk;

import android.app.Activity;
import android.app.Application;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.LocaleUtils;

import java.io.File;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import io.snabble.sdk.encodedcodes.EncodedCodesOptions;
import io.snabble.sdk.utils.IntRange;
import io.snabble.sdk.utils.JsonUtils;
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks;
import okhttp3.OkHttpClient;

public class Project {
    private String id;

    private ProductDatabase productDatabase;
    private Shop[] shops;
    private Checkout checkout;
    private ShoppingCartManager shoppingCartManager;
    private Events events;

    private List<OnProjectUpdatedListener> updateListeners = new CopyOnWriteArrayList<>();

    private Currency currency;
    private int currencyFractionDigits;
    private Locale currencyLocale;

    private String loyaltyCardId;

    private String[] pricePrefixes;
    private String[] weighPrefixes;
    private String[] unitPrefixes;
    private boolean isCheckoutAvailable;

    private EncodedCodesOptions encodedCodesOptions;

    private boolean useGermanPrintPrefix;

    private RoundingMode roundingMode;
    private boolean verifyInternalEanChecksum;
    private BarcodeFormat[] supportedBarcodeFormats;
    private Shop checkedInShop;
    private Map<BarcodeFormat, IntRange> barcodeFormatRanges;

    private Map<String, String> urls;

    private OkHttpClient okHttpClient;

    private File internalStorageDirectory;

    Project(JsonObject jsonObject) throws IllegalArgumentException {
        Snabble snabble = Snabble.getInstance();

        parse(jsonObject);

        internalStorageDirectory = new File(snabble.getInternalStorageDirectory(), id + "/");
        okHttpClient = OkHttpClientFactory.createOkHttpClient(Snabble.getInstance().getApplication(), this);

        boolean generateSearchIndex = snabble.getConfig().generateSearchIndex;

        productDatabase = new ProductDatabase(this, id + ".sqlite3", generateSearchIndex);
        shoppingCartManager = new ShoppingCartManager(this);
        checkout = new Checkout(this);
        events = new Events(this);

        Application app = Snabble.getInstance().getApplication();
        app.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    void parse(JsonObject jsonObject) {
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
        } catch (IllegalArgumentException e) {
            currencyLocale = Locale.getDefault();
        }

        if (currencyLocale == null) {
            currencyLocale = Locale.getDefault();
        }

        currencyFractionDigits = JsonUtils.getIntOpt(jsonObject, "decimalDigits", 2);

        weighPrefixes = JsonUtils.getStringArrayOpt(jsonObject, "weighPrefixes", new String[0]);
        pricePrefixes = JsonUtils.getStringArrayOpt(jsonObject, "pricePrefixes", new String[0]);
        unitPrefixes = JsonUtils.getStringArrayOpt(jsonObject, "unitPrefixes", new String[0]);

        isCheckoutAvailable = JsonUtils.getBooleanOpt(jsonObject, "enableCheckout", true);

        if (jsonObject.has("encodedCodes")) {
            JsonElement encodedCodes = jsonObject.get("encodedCodes");
            if (!encodedCodes.isJsonNull()) {
                JsonObject object = encodedCodes.getAsJsonObject();

                encodedCodesOptions = new EncodedCodesOptions.Builder()
                        .prefix(JsonUtils.getStringOpt(object, "prefix", ""))
                        .separator(JsonUtils.getStringOpt(object, "separator", "\n"))
                        .suffix(JsonUtils.getStringOpt(object, "suffix", ""))
                        .maxCodes(JsonUtils.getIntOpt(object, "maxCodes", 100))
                        .finalCode(JsonUtils.getStringOpt(object, "finalCode", ""))
                        .nextCode(JsonUtils.getStringOpt(object, "nextCode", ""))
                        .nextCodeWithCheck(JsonUtils.getStringOpt(object, "nextCodeWithCheck", ""))
                        .build();
            }
        }

        useGermanPrintPrefix = JsonUtils.getBooleanOpt(jsonObject, "useGermanPrintPrefix", false);

        String[] scanFormats = JsonUtils.getStringArrayOpt(jsonObject, "scanFormats", null);
        List<BarcodeFormat> formats = new ArrayList<>();

        if(scanFormats != null) {
            for (String scanFormat : scanFormats) {
                BarcodeFormat format = BarcodeFormat.parse(scanFormat);
                if(format != null) {
                    formats.add(format);
                }
            }
        } else {
            formats.add(BarcodeFormat.EAN_8);
            formats.add(BarcodeFormat.EAN_13);
            formats.add(BarcodeFormat.CODE_128);
        }
        supportedBarcodeFormats = formats.toArray(new BarcodeFormat[formats.size()]);

        barcodeFormatRanges = new HashMap<>();
        // TODO parse from metadata
        if (id.contains("ikea")) {
            barcodeFormatRanges.put(BarcodeFormat.ITF_14, new IntRange(0, 8));
        }

        if (jsonObject.has("shops")) {
            shops = Shop.fromJson(jsonObject.get("shops"));
        }

        if (shops == null) {
            shops = new Shop[0];
        }

        notifyUpdate();
    }

    public File getInternalStorageDirectory() {
        return internalStorageDirectory;
    }

    private RoundingMode parseRoundingMode(JsonElement jsonElement) {
        if (jsonElement != null) {
            String roundingMode = jsonElement.getAsString();
            if (roundingMode != null) {
                switch (roundingMode) {
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

    public String getProductsBySkus() {
        return urls.get("productsBySku");
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

    public BarcodeFormat[] getSupportedBarcodeFormats() {
        return supportedBarcodeFormats;
    }

    public EncodedCodesOptions getEncodedCodesOptions() {
        return encodedCodesOptions;
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
     * Sets the shop used for receiving store specific prices and identification in the
     * payment process.
     */
    public void setCheckedInShop(Shop checkedInShop) {
        this.checkedInShop = checkedInShop;
        events.updateShop(checkedInShop);
    }

    public Shop getCheckedInShop() {
        return checkedInShop;
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

    public IntRange getRangeForBarcodeFormat(BarcodeFormat barcodeFormat) {
        if (barcodeFormatRanges != null) {
            return barcodeFormatRanges.get(barcodeFormat);
        }

        return null;
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

    private void notifyUpdate() {
        for (OnProjectUpdatedListener l : updateListeners) {
            l.onProjectUpdated(this);
        }
    }

    /**
     * Adds a listener that gets called every time the metadata updates
     */
    public void addOnUpdateListener(OnProjectUpdatedListener l) {
        updateListeners.add(l);
    }

    /**
     * Removes an already added listener
     */
    public void removeOnUpdateListener(OnProjectUpdatedListener l) {
        updateListeners.remove(l);
    }

    public interface OnProjectUpdatedListener {
        void onProjectUpdated(Project project);
    }
}
