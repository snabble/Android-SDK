package io.snabble.sdk

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.Snabble.instance

import io.snabble.sdk.googlepay.GooglePayHelper
import io.snabble.sdk.encodedcodes.EncodedCodesOptions
import okhttp3.OkHttpClient
import io.snabble.sdk.codes.templates.CodeTemplate
import io.snabble.sdk.codes.templates.PriceOverrideTemplate
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.SimpleJsonCallback
import io.snabble.sdk.auth.SnabbleAuthorizationInterceptor
import io.snabble.sdk.utils.JsonUtils
import io.snabble.sdk.utils.Logger
import okhttp3.Request
import org.apache.commons.lang3.LocaleUtils
import java.io.File
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class Project internal constructor(jsonObject: JsonObject) {
    private val snabble: Snabble = instance

    var id: String = ""
        private set

    var name: String = ""
        private set

    var company: Company? = null
        private set

    var brand: Brand? = null
        private set

    var roundingMode: RoundingMode = RoundingMode.HALF_UP
        private set

    var currency: Currency = Currency.getInstance("EUR")
        private set

    var currencyLocale: Locale = Locale.GERMANY
        private set

    var isCheckoutAvailable = false
        private set

    var encodedCodesOptions: EncodedCodesOptions? = null
        private set

    var supportedBarcodeFormats: List<BarcodeFormat> = emptyList()
        private set

    private var currencyFractionDigits = 2

    var isDisplayingNetPrice = false
        private set

    var paymentMethodDescriptors: List<PaymentMethodDescriptor> = emptyList()
        private set

    var shops: List<Shop> = emptyList()

    /**
     * Returns the possible accepted cards and if a customer card is required.
     */
    var customerCardInfos: List<CustomerCardInfo> = emptyList()
        private set

    var requiredCustomerCardInfo: CustomerCardInfo? = null
        private set

    var customerCardId: String? = null
        set(value) {
            field = value

            // refresh encoded codes options for encoded codes that contain customer cards
            if (encodedCodesJsonObject != null) {
                encodedCodesOptions = EncodedCodesOptions.fromJsonObject(this, encodedCodesJsonObject)
            }
        }

    /** The limit of online payments, in cents (or other base currency values)  */
    var maxOnlinePaymentLimit = 0
        private set

    /** The limit of all checkout methods, in cents (or other base currency values)  */
    var maxCheckoutLimit = 0
        private set

    var codeTemplates: List<CodeTemplate> = emptyList()
        private set

    var priceOverrideTemplates: List<PriceOverrideTemplate> = emptyList()
        private set

    var searchableTemplates: List<String> = emptyList()
        private set

    var priceFormatter: PriceFormatter? = null
        private set

    var tokensUrl: String? = null
        private set

    var appUserUrl: String? = null
        private set

    /**
     * A key-value map containing urls provided by the metadata.
     * All urls are absolute, even if the original metadata contained relative urls.
     */
    var urls: Map<String, String> = emptyMap()
        private set

    /**
     * Sets the shop used for receiving store specific prices and identification in the
     * payment process.
     */
    var checkedInShop: Shop? = null
        set(checkedInShop) {
            val currentShopId = if (this.checkedInShop != null) this.checkedInShop!!.id else ""
            val newShopId = if (checkedInShop != null) checkedInShop.id else ""
            if (currentShopId != newShopId) {
                field = checkedInShop
                if (newShopId == "") {
                    snabble.userPreferences.lastCheckedInShopId = null
                } else {
                    snabble.userPreferences.lastCheckedInShopId = newShopId
                }
                events!!.updateShop(checkedInShop)
                shoppingCart.updatePrices(false)
            }
        }

    private var texts: MutableMap<String, String> = mutableMapOf()

    private var encodedCodesJsonObject: JsonObject? = null

    private val updateListeners: MutableList<OnProjectUpdatedListener> = CopyOnWriteArrayList()

    private val shoppingCartStorage: ShoppingCartStorage

    val internalStorageDirectory: File

    val okHttpClient: OkHttpClient

    val coupons: Coupons

    val checkout: Checkout

    val productDatabase: ProductDatabase

    val events: Events?

    val assets: Assets

    var googlePayHelper: GooglePayHelper? = null

    fun parse(jsonObject: JsonObject) {
        val urls: MutableMap<String, String> = HashMap()
        id = if (jsonObject.has("id")) {
            jsonObject["id"].asString
        } else {
            throw IllegalArgumentException("Project has no id")
        }
        name = JsonUtils.getStringOpt(jsonObject, "name", id)
        val brandId = JsonUtils.getStringOpt(jsonObject, "brandID", null)
        if (brandId != null) {
            brand = snabble.brands[brandId]
        }
        val links = jsonObject["links"].asJsonObject
        val linkKeys = links.keySet()
        for (k in linkKeys) {
            urls[k] = snabble.absoluteUrl(links[k].asJsonObject["href"].asString)
        }
        this.urls = Collections.unmodifiableMap(urls)
        tokensUrl = urls["tokens"].toString() + "?role=retailerApp"
        appUserUrl = snabble.createAppUserUrl + "?project=" + id
        roundingMode = parseRoundingMode(jsonObject["roundingMode"])
        currency = Currency.getInstance(JsonUtils.getStringOpt(jsonObject, "currency", "EUR"))
        val locale = JsonUtils.getStringOpt(jsonObject, "locale", "de_DE")
        currencyLocale = try {
            LocaleUtils.toLocale(locale)
        } catch (e: IllegalArgumentException) {
            Locale.getDefault()
        }
        if (currencyLocale == null) {
            currencyLocale = Locale.getDefault()
        }
        currencyFractionDigits = JsonUtils.getIntOpt(jsonObject, "decimalDigits", 2)
        priceFormatter = PriceFormatter(this)
        isCheckoutAvailable = JsonUtils.getBooleanOpt(jsonObject, "enableCheckout", true)
        if (jsonObject.has("qrCodeOffline")) {
            val encodedCodes = jsonObject["qrCodeOffline"]
            if (!encodedCodes.isJsonNull) {
                encodedCodesJsonObject = encodedCodes.asJsonObject
                encodedCodesOptions =
                    EncodedCodesOptions.fromJsonObject(this, encodedCodesJsonObject)
            }
        }
        val scanFormats = JsonUtils.getStringArrayOpt(jsonObject, "scanFormats", null)
        val formats: MutableList<BarcodeFormat> = ArrayList()
        if (scanFormats != null) {
            for (scanFormat in scanFormats) {
                val format = BarcodeFormat.parse(scanFormat)
                if (format != null) {
                    formats.add(format)
                }
            }
        } else {
            formats.add(BarcodeFormat.EAN_8)
            formats.add(BarcodeFormat.EAN_13)
            formats.add(BarcodeFormat.CODE_128)
        }
        supportedBarcodeFormats = formats
        val customerCards = jsonObject.getAsJsonObject("customerCards")
        val acceptedCustomerCards =
            JsonUtils.getStringArrayOpt(customerCards, "accepted", arrayOfNulls(0))
        val requiredCustomerCard = JsonUtils.getStringOpt(customerCards, "required", null)
        val customerCardInfos: MutableList<CustomerCardInfo> = ArrayList()
        for (id in acceptedCustomerCards) {
            var required = false
            if (id == requiredCustomerCard) {
                required = true
            }
            val customerCardInfo = CustomerCardInfo(id, required)
            customerCardInfos.add(customerCardInfo)
        }
        this.customerCardInfos = customerCardInfos
        if (requiredCustomerCard != null) {
            requiredCustomerCardInfo = CustomerCardInfo(requiredCustomerCard, true)
        }
        val descriptors = jsonObject["paymentMethodDescriptors"]
        if (descriptors != null) {
            val t = object : TypeToken<List<PaymentMethodDescriptor?>?>() {}.type
            val paymentMethodDescriptors =
                GsonHolder.get().fromJson<List<PaymentMethodDescriptor>>(descriptors, t)
            val filteredDescriptors = ArrayList<PaymentMethodDescriptor>()
            for (descriptor in paymentMethodDescriptors) {
                if (PaymentMethod.fromString(descriptor.id) != null) {
                    filteredDescriptors.add(descriptor)
                }
            }
            this.paymentMethodDescriptors = Collections.unmodifiableList(filteredDescriptors)
        } else {
            paymentMethodDescriptors = Collections.unmodifiableList(emptyList())
        }
        parseShops(jsonObject)
        if (jsonObject.has("company")) {
            company = GsonHolder.get().fromJson(jsonObject["company"], Company::class.java)
        }
        val codeTemplates = ArrayList<CodeTemplate>()
        if (jsonObject.has("codeTemplates")) {
            for ((key, value) in jsonObject["codeTemplates"].asJsonObject.entrySet()) {
                try {
                    val codeTemplate = CodeTemplate(key, value.asString)
                    codeTemplates.add(codeTemplate)
                } catch (e: Exception) {
                    Logger.e("Could not parse template %s: %s", key, e.message)
                }
            }
        }
        var hasDefaultTemplate = false
        for (codeTemplate in codeTemplates) {
            if ("default" == codeTemplate.name) {
                hasDefaultTemplate = true
                break
            }
        }
        if (!hasDefaultTemplate) {
            codeTemplates.add(CodeTemplate("default", "{code:*}"))
        }
        this.codeTemplates = codeTemplates
        val priceOverrideTemplates: MutableList<PriceOverrideTemplate> = ArrayList()
        if (jsonObject.has("priceOverrideCodes")) {
            val priceOverrideCodes = jsonObject["priceOverrideCodes"].asJsonArray
            for (element in priceOverrideCodes) {
                val priceOverride = element.asJsonObject
                try {
                    val codeTemplate = CodeTemplate(
                        priceOverride["id"].asString,
                        priceOverride["template"].asString
                    )
                    var matchingTemplate: CodeTemplate? = null
                    if (priceOverride.has("transmissionTemplate")) {
                        matchingTemplate =
                            getCodeTemplate(priceOverride["transmissionTemplate"].asString)
                    }
                    val priceOverrideTemplate = PriceOverrideTemplate(
                        codeTemplate,
                        matchingTemplate,
                        JsonUtils.getStringOpt(priceOverride, "transmissionCode", null)
                    )
                    priceOverrideTemplates.add(priceOverrideTemplate)
                } catch (e: Exception) {
                    Logger.e("Could not parse priceOverrideTemplate %s", e.message)
                }
            }
        }
        this.priceOverrideTemplates = priceOverrideTemplates
        searchableTemplates = JsonUtils.getStringArrayOpt(jsonObject, "searchableTemplates", arrayOf("default")).toList()

        if (jsonObject.has("checkoutLimits")) {
            val checkoutLimits = jsonObject.getAsJsonObject("checkoutLimits")
            maxCheckoutLimit = JsonUtils.getIntOpt(checkoutLimits, "checkoutNotAvailable", 0)
            maxOnlinePaymentLimit = JsonUtils.getIntOpt(checkoutLimits, "notAllMethodsAvailable", 0)
        }
        texts = HashMap()
        if (jsonObject.has("texts")) {
            val textsElement = jsonObject["texts"]
            if (!textsElement.isJsonNull) {
                val textsJsonObject = jsonObject["texts"].asJsonObject
                for ((key, value) in textsJsonObject.entrySet()) {
                    texts[key] = value.asString
                }
            }
        }
        isDisplayingNetPrice = JsonUtils.getBooleanOpt(jsonObject, "displayNetPrice", false)
        var couponList: List<Coupon> = ArrayList()
        try {
            if (jsonObject.has("coupons")) {
                val couponsJsonObject = jsonObject["coupons"]
                val couponsType = object : TypeToken<List<Coupon?>?>() {}.type
                couponList = GsonHolder.get().fromJson(couponsJsonObject, couponsType)
            }
        } catch (e: Exception) {
            Logger.e("Could not parse coupons")
        }
        if (coupons.source.value !== CouponSource.Online) {
            coupons.setProjectCoupons(couponList)
        }
        coupons.update()
        notifyUpdate()
    }

    private fun parseShops(jsonObject: JsonObject) {
        shops = if (jsonObject.has("shops")) {
            Shop.fromJson(jsonObject["shops"])?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun loadActiveShops(done: Runnable?) {
        if (snabble.config.loadActiveShops) {
            val url = activeShopsUrl
            if (url != null) {
                val request: Request = Request.Builder()
                    .get()
                    .url(url)
                    .build()
                okHttpClient.newCall(request).enqueue(object : SimpleJsonCallback<JsonObject>(
                    JsonObject::class.java
                ) {
                    override fun success(jsonObject: JsonObject) {
                        jsonObject["shops"]?.let {
                            shops = Shop.fromJson(it).toList()
                        }

                        done?.run()
                    }

                    override fun error(t: Throwable) {
                        Logger.e("Failed to load hidden shops, statusCode: %d", responseCode())
                    }
                })
            }
        }
    }

    private fun parseRoundingMode(jsonElement: JsonElement?): RoundingMode {
        if (jsonElement != null) {
            val roundingMode = jsonElement.asString
            if (roundingMode != null) {
                when (roundingMode) {
                    "up" -> return RoundingMode.UP
                    "down" -> return RoundingMode.DOWN
                    "commercial" -> return RoundingMode.HALF_UP
                }
            }
        }
        return RoundingMode.HALF_UP
    }

    val eventsUrl: String?
        get() = urls!!["appEvents"]
    val appDbUrl: String?
        get() = urls!!["appdb"]
    val checkoutUrl: String?
        get() = urls!!["checkoutInfo"]
    val assetsUrl: String?
        get() = urls!!["assetsManifest"]
    val productBySkuUrl: String?
        get() = urls!!["resolvedProductBySku"]
    val productByCodeUrl: String?
        get() = urls!!["resolvedProductLookUp"]
    val telecashVaultItemsUrl: String?
        get() = urls!!["telecashVaultItems"]
    val datatransTokenizationUrl: String?
        get() = urls!!["datatransTokenization"]
    val activeShopsUrl: String?
        get() = urls!!["activeShops"]

    fun getText(key: String): String? {
        return getText(key, null)
    }

    fun getText(key: String, defaultValue: String?): String? {
        return texts!![key] ?: return defaultValue
    }

    val shoppingCart: ShoppingCart
        get() = shoppingCartStorage.shoppingCart

    fun getCurrencyFractionDigits(): Int {
        return if (currencyFractionDigits == -1) {
            currency!!.defaultFractionDigits
        } else {
            currencyFractionDigits
        }
    }

    fun logErrorEvent(format: String?, vararg args: Any?) {
        if (events != null) {
            Logger.e(format, *args)
            events.logError(format, *args)
        }
    }

    fun logEvent(format: String?, vararg args: Any?) {
        if (events != null) {
            Logger.e(format, *args)
            events.log(format, *args)
        }
    }

    /**
     * Sets the customer card number for user identification with the backend.
     */

    val availablePaymentMethods: List<PaymentMethod>
        get() {
            val list: MutableList<PaymentMethod> = ArrayList()
            for (descriptor in paymentMethodDescriptors!!) {
                list.add(descriptor.paymentMethod)
            }
            return list
        }

    val defaultCodeTemplate: CodeTemplate?
        get() = getCodeTemplate("default")

    fun getCodeTemplate(name: String): CodeTemplate? {
        for (codeTemplate in codeTemplates) {
            if (codeTemplate.name == name) {
                return codeTemplate
            }
        }
        return null
    }

    fun getTransformationTemplate(name: String): CodeTemplate? {
        for (priceOverrideTemplate in priceOverrideTemplates) {
            val codeTemplate = priceOverrideTemplate.transmissionCodeTemplate
            if (codeTemplate != null && codeTemplate.name == name) {
                return codeTemplate
            }
        }
        return null
    }

    private fun notifyUpdate() {
        for (l in updateListeners) {
            l.onProjectUpdated(this)
        }
    }

    /**
     * Adds a listener that gets called every time the metadata updates
     */
    fun addOnUpdateListener(l: OnProjectUpdatedListener) {
        updateListeners.add(l)
    }

    /**
     * Removes an already added listener
     */
    fun removeOnUpdateListener(l: OnProjectUpdatedListener) {
        updateListeners.remove(l)
    }

    interface OnProjectUpdatedListener {
        fun onProjectUpdated(project: Project?)
    }

    init {
        coupons = Coupons(this)
        okHttpClient = instance.okHttpClient
            .newBuilder()
            .addInterceptor(SnabbleAuthorizationInterceptor(this))
            .addInterceptor(AcceptedLanguageInterceptor())
            .build()
        parse(jsonObject)
        internalStorageDirectory = File(snabble.internalStorageDirectory, "$id/")
        val generateSearchIndex = snabble.config.generateSearchIndex
        productDatabase = ProductDatabase(this, "$id.sqlite3", generateSearchIndex)
        shoppingCartStorage = ShoppingCartStorage(this)
        checkout = Checkout(this)
        events = Events(this)
        assets = Assets(this)
        for (descriptor in paymentMethodDescriptors!!) {
            if (descriptor.paymentMethod == PaymentMethod.GOOGLE_PAY) {
                googlePayHelper = GooglePayHelper(this, instance.application)
                break
            }
        }
    }
}