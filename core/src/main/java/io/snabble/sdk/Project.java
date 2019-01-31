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

import io.snabble.sdk.codes.templates.CodeTemplate;
import io.snabble.sdk.codes.templates.PriceOverrideTemplate;
import io.snabble.sdk.encodedcodes.EncodedCodesOptions;
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

    private List<OnProjectUpdatedListener> updateListeners = new CopyOnWriteArrayList<>();

    private Currency currency;
    private int currencyFractionDigits;
    private Locale currencyLocale;

    private String loyaltyCardId;
    private boolean isCheckoutAvailable;

    private EncodedCodesOptions encodedCodesOptions;

    private RoundingMode roundingMode;
    private BarcodeFormat[] supportedBarcodeFormats;
    private Shop checkedInShop;
    private CustomerCardInfo[] acceptedCustomerCardInfos;
    private CustomerCardInfo requiredCustomerCardInfo;

    private Map<String, String> urls;

    private OkHttpClient okHttpClient;

    private File internalStorageDirectory;

    private CodeTemplate[] codeTemplates;
    private PriceOverrideTemplate[] priceOverrideTemplates;
    private String[] searchableTemplates;

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
        isCheckoutAvailable = JsonUtils.getBooleanOpt(jsonObject, "enableCheckout", true);

        if (jsonObject.has("encodedCodes")) {
            JsonElement encodedCodes = jsonObject.get("encodedCodes");
            if (!encodedCodes.isJsonNull()) {
                JsonObject object = encodedCodes.getAsJsonObject();

                encodedCodesOptions = new EncodedCodesOptions.Builder(this)
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

        JsonObject customerCards = jsonObject.getAsJsonObject("customerCards");
        String[] acceptedCustomerCards = JsonUtils.getStringArrayOpt(customerCards, "accepted", new String[0]);
        String requiredCustomerCard = JsonUtils.getStringOpt(customerCards, "required", null);
        acceptedCustomerCardInfos = new CustomerCardInfo[acceptedCustomerCards.length];

        for (int i=0; i<acceptedCustomerCards.length; i++) {
            String id = acceptedCustomerCards[i];

            boolean required = false;
            if (id.equals(requiredCustomerCard)) {
                required = true;
            }

            CustomerCardInfo customerCardInfo = new CustomerCardInfo(id, required);
            acceptedCustomerCardInfos[i] = customerCardInfo;

            if (required) {
                requiredCustomerCardInfo = customerCardInfo;
            }
        }

        if (jsonObject.has("shops")) {
            shops = Shop.fromJson(jsonObject.get("shops"));
        }

        if (shops == null) {
            shops = new Shop[0];
        }

        // TODO parse from metadata 
        ArrayList<CodeTemplate> codeTemplates = new ArrayList<>();

        codeTemplates.add(new CodeTemplate("ean13_instore_chk", "2{code:5}{i}{embed:5}{_}"));
        codeTemplates.add(new CodeTemplate("ean13_instore",  "2{code:5}{_}{embed:5}{_}"));
        codeTemplates.add(new CodeTemplate("german_print", "4{code:2}{_:5}{embed:4}{_}"));
        codeTemplates.add(new CodeTemplate("ean14_code128", "01{code:ean14}"));
        codeTemplates.add(new CodeTemplate("globus_unitrade_ww", "94{code:5}{_:19}"));
        codeTemplates.add(new CodeTemplate("globus_unitrade", "94{code:3}{_:10}"));
        codeTemplates.add(new CodeTemplate("globus_unitrade_rep_1", "96{code:2}{_:36}"));
        codeTemplates.add(new CodeTemplate("globus_unitrade_rep_2", "96{_:13}{code:3}{_:30}"));
        codeTemplates.add(new CodeTemplate("globus_weighing", "96{code:ean13}{embed:7}{price:5}{_}"));
        codeTemplates.add(new CodeTemplate("ikea_itf14", "{code:8}{_:6}"));
        codeTemplates.add(new CodeTemplate("default", "{code:*}"));

        this.codeTemplates = codeTemplates.toArray(new CodeTemplate[codeTemplates.size()]);

        List<PriceOverrideTemplate> priceOverrideTemplates = new ArrayList<>();
        priceOverrideTemplates.add(new PriceOverrideTemplate(
                new CodeTemplate("edeka_discount", "97{code:ean13}{embed:6}{_}"),
                new CodeTemplate("edeka_discount_override", "241700{i}{embed:5}{ec}")));

        if (id.contains("ikea")) {
            priceOverrideTemplates.add(new PriceOverrideTemplate(
                    new CodeTemplate("ikea_fundgrube", "{_}{_:7}{_}{_:17}{_}{_:3}{code:8}{_}{_:9}{embed100:5}{_}"),
                    null));
        }

        if (id.contains("globus")) {
            priceOverrideTemplates.add(new PriceOverrideTemplate(
                    new CodeTemplate("globus_discount", "98{code:ean13}{_:8}{embed:7}{_:2}"),
                    null));
        }

        this.priceOverrideTemplates = priceOverrideTemplates.toArray(new PriceOverrideTemplate[priceOverrideTemplates.size()]);

        List<String> searchableTemplates = new ArrayList<>();
        searchableTemplates.add("default");

        if (id.contains("ikea")) {
            searchableTemplates.add("ikea_itf14");
        }

        this.searchableTemplates = searchableTemplates.toArray(new String[searchableTemplates.size()]);

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
        return urls.get("resolvedProductBySku");
    }

    public String getProductByCodeUrl() {
        return urls.get("resolvedProductLookUp");
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

    public OkHttpClient getOkHttpClient() {
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

    public Checkout getCheckout() {
        return checkout;
    }

    public Events getEvents() {
        return events;
    }

    public void logErrorEvent(String format, Object... args) {
        if (events != null) {
            Logger.e(format, args);
            events.logError(format, args);
        }
    }

    /**
     * Sets the customer card number for user identification with the backend.
     */
    public void setCustomerCardId(String loyaltyCardId) {
        this.loyaltyCardId = loyaltyCardId;
    }

    public String getCustomerCardId() {
        return loyaltyCardId;
    }

    public CodeTemplate getCodeTemplate(String name) {
        for (CodeTemplate codeTemplate : codeTemplates) {
            if (codeTemplate.getName().equals(name)) {
                return codeTemplate;
            }
        }

        return null;
    }

    public CodeTemplate[] getCodeTemplates() {
        return codeTemplates;
    }

    public PriceOverrideTemplate[] getPriceOverrideTemplates() {
        return priceOverrideTemplates;
    }

    public CodeTemplate getTransformationTemplate(String name) {
        for (PriceOverrideTemplate priceOverrideTemplate : priceOverrideTemplates) {
            CodeTemplate codeTemplate = priceOverrideTemplate.getTransmissionCodeTemplate();
            if (codeTemplate != null && codeTemplate.getName().equals(name)) {
                return codeTemplate;
            }
        }

        return null;
    }

    public String[] getSearchableTemplates() {
        return searchableTemplates;
    }

    /**
     * Returns the possible accepted cards and if a customer card is required.
     */
    public CustomerCardInfo[] getCustomerCardInfos() {
        return acceptedCustomerCardInfos;
    }

    public CustomerCardInfo getRequiredCustomerCardInfo() {
        return requiredCustomerCardInfo;
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
