package io.snabble.sdk;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.LocaleUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import io.snabble.sdk.googlepay.GooglePayHelper;
import io.snabble.sdk.auth.SnabbleAuthorizationInterceptor;
import io.snabble.sdk.codes.templates.CodeTemplate;
import io.snabble.sdk.codes.templates.PriceOverrideTemplate;
import io.snabble.sdk.encodedcodes.EncodedCodesOptions;
import io.snabble.sdk.utils.GsonHolder;
import io.snabble.sdk.utils.JsonUtils;
import io.snabble.sdk.utils.Logger;
import okhttp3.OkHttpClient;

public class Project {
    private Snabble snabble;
    private String id;
    private String name;

    private ProductDatabase productDatabase;
    private Shop[] shops;
    private Brand brand;
    private Company company;
    private Checkout checkout;
    private ShoppingCartStorage shoppingCartStorage;
    private Events events;
    private Assets assets;
    private GooglePayHelper googlePayHelper;
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
    private PaymentMethod[] availablePaymentMethods;

    private Map<String, String> urls;

    private OkHttpClient okHttpClient;

    private File internalStorageDirectory;

    private CodeTemplate[] codeTemplates;
    private PriceOverrideTemplate[] priceOverrideTemplates;
    private String[] searchableTemplates;
    private PriceFormatter priceFormatter;
    private Coupons coupons;
    private Map<String, String> texts;

    private int maxOnlinePaymentLimit;
    private int maxCheckoutLimit;
    private JsonObject encodedCodesJsonObject;
    private String tokensUrl;
    private String appUserUrl;
    private boolean displayNetPrice;

    Project(JsonObject jsonObject) throws IllegalArgumentException {
        snabble = Snabble.getInstance();

        parse(jsonObject);

        internalStorageDirectory = new File(snabble.getInternalStorageDirectory(), id + "/");
        okHttpClient = Snabble.getInstance().getOkHttpClient()
                .newBuilder()
                .addInterceptor(new SnabbleAuthorizationInterceptor(this))
                .build();

        boolean generateSearchIndex = snabble.getConfig().generateSearchIndex;

        productDatabase = new ProductDatabase(this, id + ".sqlite3", generateSearchIndex);
        shoppingCartStorage = new ShoppingCartStorage(this);
        checkout = new Checkout(this);
        events = new Events(this);
        assets = new Assets(this);

        for (PaymentMethod paymentMethod : getAvailablePaymentMethods()) {
            if (paymentMethod == PaymentMethod.GOOGLE_PAY) {
                googlePayHelper = new GooglePayHelper(this, Snabble.getInstance().getApplication());
                break;
            }
        }
    }

    void parse(JsonObject jsonObject) {
        Snabble snabble = Snabble.getInstance();

        Map<String, String> urls = new HashMap<>();

        if (jsonObject.has("id")) {
            id = jsonObject.get("id").getAsString();
        } else {
            throw new IllegalArgumentException("Project has no id");
        }

        name = JsonUtils.getStringOpt(jsonObject, "name", id);

        String brandId = JsonUtils.getStringOpt(jsonObject, "brandID", null);
        if (brandId != null) {
            brand = snabble.getBrands().get(brandId);
        }

        JsonObject links = jsonObject.get("links").getAsJsonObject();
        Set<String> linkKeys = links.keySet();
        for (String k : linkKeys) {
            urls.put(k, snabble.absoluteUrl(links.get(k).getAsJsonObject().get("href").getAsString()));
        }

        this.urls = Collections.unmodifiableMap(urls);
        this.tokensUrl = urls.get("tokens") + "?role=retailerApp";
        this.appUserUrl = snabble.getCreateAppUserUrl() + "?project=" + id;

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
        priceFormatter = new PriceFormatter(this);

        isCheckoutAvailable = JsonUtils.getBooleanOpt(jsonObject, "enableCheckout", true);

        if (jsonObject.has("qrCodeOffline")) {
            JsonElement encodedCodes = jsonObject.get("qrCodeOffline");
            if (!encodedCodes.isJsonNull()) {
                encodedCodesJsonObject = encodedCodes.getAsJsonObject();
                encodedCodesOptions = EncodedCodesOptions.fromJsonObject(this, encodedCodesJsonObject);
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

        List<CustomerCardInfo> customerCardInfos = new ArrayList<>();
        for (String id : acceptedCustomerCards) {
            boolean required = false;
            if (id.equals(requiredCustomerCard)) {
                required = true;
            }

            CustomerCardInfo customerCardInfo = new CustomerCardInfo(id, required);
            customerCardInfos.add(customerCardInfo);
        }

        acceptedCustomerCardInfos = customerCardInfos.toArray(new CustomerCardInfo[customerCardInfos.size()]);

        if (requiredCustomerCard != null) {
            requiredCustomerCardInfo = new CustomerCardInfo(requiredCustomerCard, true);
        }

        List<PaymentMethod> paymentMethodList = new ArrayList<>();
        String[] paymentMethods = JsonUtils.getStringArrayOpt(jsonObject, "paymentMethods", new String[0]);
        for (String paymentMethod : paymentMethods) {
            PaymentMethod pm = PaymentMethod.fromString(paymentMethod);
            if (pm != null) {
                paymentMethodList.add(pm);
            }
        }

        availablePaymentMethods = paymentMethodList.toArray(new PaymentMethod[paymentMethodList.size()]);

        if (jsonObject.has("shops")) {
            shops = Shop.fromJson(jsonObject.get("shops"));
        }

        if (shops == null) {
            shops = new Shop[0];
        }

        if (jsonObject.has("company")) {
            company = GsonHolder.get().fromJson(jsonObject.get("company"), Company.class);
        }

        ArrayList<CodeTemplate> codeTemplates = new ArrayList<>();
        if (jsonObject.has("codeTemplates")) {
            for (Map.Entry<String, JsonElement> entry : jsonObject.get("codeTemplates").getAsJsonObject().entrySet()) {
                try {
                    CodeTemplate codeTemplate = new CodeTemplate(entry.getKey(), entry.getValue().getAsString());
                    codeTemplates.add(codeTemplate);
                } catch (Exception e) {
                    Logger.e("Could not parse template %s: %s", entry.getKey(), e.getMessage());
                }
            }
        }

        boolean hasDefaultTemplate = false;
        for (CodeTemplate codeTemplate : codeTemplates) {
            if ("default".equals(codeTemplate.getName())) {
                hasDefaultTemplate = true;
                break;
            }
        }

        if (!hasDefaultTemplate) {
            codeTemplates.add(new CodeTemplate("default", "{code:*}"));
        }
        
        this.codeTemplates = codeTemplates.toArray(new CodeTemplate[codeTemplates.size()]);

        List<PriceOverrideTemplate> priceOverrideTemplates = new ArrayList<>();
        if (jsonObject.has("priceOverrideCodes")) {
            JsonArray priceOverrideCodes = jsonObject.get("priceOverrideCodes").getAsJsonArray();
            for (JsonElement element : priceOverrideCodes) {
                JsonObject priceOverride = element.getAsJsonObject();
                try {
                    CodeTemplate codeTemplate = new CodeTemplate(priceOverride.get("id").getAsString(),
                            priceOverride.get("template").getAsString());

                    CodeTemplate matchingTemplate = null;
                    if (priceOverride.has("transmissionTemplate")) {
                        matchingTemplate = getCodeTemplate(priceOverride.get("transmissionTemplate").getAsString());
                    }

                    PriceOverrideTemplate priceOverrideTemplate = new PriceOverrideTemplate(codeTemplate,
                            matchingTemplate, JsonUtils.getStringOpt(priceOverride, "transmissionCode", null));

                    priceOverrideTemplates.add(priceOverrideTemplate);
                } catch (Exception e) {
                    Logger.e("Could not parse priceOverrideTemplate %s", e.getMessage());
                }
            }
        }

        this.priceOverrideTemplates = priceOverrideTemplates.toArray(new PriceOverrideTemplate[priceOverrideTemplates.size()]);

        searchableTemplates = JsonUtils.getStringArrayOpt(jsonObject, "searchableTemplates", new String[] { "default" });

        if (jsonObject.has("checkoutLimits")) {
            JsonObject checkoutLimits = jsonObject.getAsJsonObject("checkoutLimits");
            maxCheckoutLimit = JsonUtils.getIntOpt(checkoutLimits, "checkoutNotAvailable", 0);
            maxOnlinePaymentLimit = JsonUtils.getIntOpt(checkoutLimits, "notAllMethodsAvailable", 0);
        }

        texts = new HashMap<>();

        if (jsonObject.has("texts")) {
            JsonElement textsElement = jsonObject.get("texts");
            if (!textsElement.isJsonNull()) {
                JsonObject textsJsonObject = jsonObject.get("texts").getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : textsJsonObject.entrySet()) {
                    texts.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }

        displayNetPrice = JsonUtils.getBooleanOpt(jsonObject, "displayNetPrice", false);


        List<Coupon> couponList = new ArrayList<>();

        try {
            if (jsonObject.has("coupons")) {
                JsonElement couponsJsonObject = jsonObject.get("coupons");
                Type couponsType = new TypeToken<List<Coupon>>() {}.getType();
                couponList = GsonHolder.get().fromJson(couponsJsonObject, couponsType);
            }
        } catch (Exception e) {
            Logger.e("Could not parse coupons");
        }

        this.coupons = new Coupons(couponList);

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

    public String getName() {
        return name;
    }

    public String getTokensUrl() {
        return tokensUrl;
    }

    public String getAppUserUrl() {
        return appUserUrl;
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

    public String getAssetsUrl() {
        return urls.get("assetsManifest");
    }

    public String getProductBySkuUrl() {
        return urls.get("resolvedProductBySku");
    }

    public String getProductByCodeUrl() {
        return urls.get("resolvedProductLookUp");
    }

    public String getTelecashVaultItemsUrl() {
        return urls.get("telecashVaultItems");
    }

    public String getDatatransTokenizationUrl() {
        return urls.get("datatransTokenization");
    }

    public String getShoppingListDbUrl() {
        return urls.get("shoppingListDB");
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

    public boolean isDisplayingNetPrice() {
        return displayNetPrice;
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

    public String getText(String key) {
        return getText(key, null);
    }

    public String getText(String key, String defaultValue) {
        String text = texts.get(key);
        if (text == null) {
            return defaultValue;
        }

        return text;
    }

    /**
     * Sets the shop used for receiving store specific prices and identification in the
     * payment process.
     */
    public void setCheckedInShop(Shop checkedInShop) {
        this.checkedInShop = checkedInShop;
        events.updateShop(checkedInShop);
        getShoppingCart().updatePrices(false);
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

    public Company getCompany() {
        return company;
    }

    public ShoppingCart getShoppingCart() {
        return shoppingCartStorage.getShoppingCart();
    }

    public Currency getCurrency() {
        return currency;
    }

    public Brand getBrand() {
        return brand;
    }

    public PriceFormatter getPriceFormatter() {
        return priceFormatter;
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

    public Assets getAssets() {
        return assets;
    }

    public Coupons getCoupons() {
        return coupons;
    }

    @Nullable
    public GooglePayHelper getGooglePayHelper() {
        return googlePayHelper;
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

        if (encodedCodesJsonObject != null) {
            encodedCodesOptions = EncodedCodesOptions.fromJsonObject(this, encodedCodesJsonObject);
        }
    }

    public String getCustomerCardId() {
        return loyaltyCardId;
    }

    public PaymentMethod[] getAvailablePaymentMethods() {
        return availablePaymentMethods;
    }

    public CodeTemplate getDefaultCodeTemplate() {
        return getCodeTemplate("default");
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

    /** The limit of online payments, in cents (or other base currency values) **/
    public int getMaxOnlinePaymentLimit() {
        return maxOnlinePaymentLimit;
    }

    /** The limit of all checkout methods, in cents (or other base currency values) **/
    public int getMaxCheckoutLimit() {
        return maxCheckoutLimit;
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
