package io.snabble.sdk

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.Snabble.instance

import io.snabble.sdk.googlepay.GooglePayHelper
import io.snabble.sdk.encodedcodes.EncodedCodesOptions
import io.snabble.sdk.codes.templates.CodeTemplate
import io.snabble.sdk.codes.templates.PriceOverrideTemplate
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.SimpleJsonCallback
import io.snabble.sdk.auth.SnabbleAuthorizationInterceptor
import io.snabble.sdk.utils.JsonUtils.getBooleanOpt
import io.snabble.sdk.utils.JsonUtils.getIntOpt
import io.snabble.sdk.utils.JsonUtils.getString
import io.snabble.sdk.utils.JsonUtils.getStringListOpt
import io.snabble.sdk.utils.JsonUtils.getStringOpt
import io.snabble.sdk.utils.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.lang3.LocaleUtils
import java.io.File
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class Project internal constructor(jsonObject: JsonObject) {
    /**
     * The unique identifier of the Project.
     */
    lateinit var id: String
        private set

    /**
     * The user facing name of the Project.
     *
     * For example used in the folding of payment methods
     */
    var name: String = ""
        private set

    var company: Company? = null
        private set

    var brand: Brand? = null
        private set

    /**
     * The roundingMode which should be used when doing offline calculations.
     */
    var roundingMode: RoundingMode = RoundingMode.HALF_UP
        private set

    /**
     * The currency used to calculate and display prices
     */
    var currency: Currency = Currency.getInstance("EUR")
        private set

    /**
     * The locale in which this currency is used.
     */
    var currencyLocale: Locale = Locale.GERMANY
        private set

    /**
     * The number of used currency fractions digits.
     */
    var currencyFractionDigits = -1
        get() =
            if (field == -1) {
                currency.defaultFractionDigits
            } else {
                field
            }
    /**
     * Indicator if checkout should be available when using this Project
     */
    var isCheckoutAvailable = false
        private set

    /**
     * Parameters in which codes used for offline processing
     * (for example: Checkout using a QR-Code) should be encoded.
     */
    var encodedCodesOptions: EncodedCodesOptions? = null
        private set

    /**
     * List of supported barcode formats used by this retailer. The scanner should restrict its
     * scanning to include only those formats.
     */
    var supportedBarcodeFormats: List<BarcodeFormat> = emptyList()
        private set

    /**
     * Indicator if prices should be displayed using the net price instead of the gross price.
     */
    var isDisplayingNetPrice = false
        private set

    /**
     * List of payment method descriptors indicating which
     * payment methods and providers are available in the Project.
     */
    var paymentMethodDescriptors: List<PaymentMethodDescriptor> = emptyList()
        private set

    /**
     * List of shops which are available.
     *
     * If using config.loadActiveShops = true also includes shops that are hidden.
     *
     * Shops that are hidden will get loaded asynchronously.
     */
    var shops: List<Shop> = emptyList()

    /**
     * Returns the possible accepted cards and if a customer card is required.
     */
    var customerCardInfos: List<CustomerCardInfo> = emptyList()
        private set

    /**
     * If a customer card is strictly required to use the checkout functionality at all.
     *
     * Note: Since customer cards are not included in the SDK, checks for required customer cards
     * need to be placed before allowing checkout.
     */
    var requiredCustomerCardInfo: CustomerCardInfo? = null
        private set

    /**
     * Sets the customer card number for user identification with the backend.
     */
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

    /**
     * List of code templates used for parsing vendor specific barcodes.
     *
     * Can be used to extract data from barcodes (e.g. price in a printed barcode)
     */
    var codeTemplates: List<CodeTemplate> = emptyList()
        private set

    /**
     * List of code templates that are used when supplying an existing Product with a different
     * barcode which contains a reduced price.
     */
    var priceOverrideTemplates: List<PriceOverrideTemplate> = emptyList()
        private set

    /**
     * List of code templates that are searchable using the barcode search functionality.
     */
    var searchableTemplates: List<String> = emptyList()
        private set

    /**
     * A price formatter for formatting prices using the provided currency information.
     *
     * Also provides functions to display embedded prices of a scanned code.
     */
    var priceFormatter: PriceFormatter = PriceFormatter(this)
        private set

    /**
     * A key-value map containing urls provided by the metadata.
     * All urls are absolute, even if the original metadata contained relative urls.
     */
    var urls: Map<String, String> = emptyMap()
        private set

    var tokensUrl: String? = null
        private set

    var appUserUrl: String? = null
        private set

    val eventsUrl: String?
        get() = urls["appEvents"]

    val appDbUrl: String?
        get() = urls["appdb"]

    val checkoutUrl: String?
        get() = urls["checkoutInfo"]

    val assetsUrl: String?
        get() = urls["assetsManifest"]

    val productBySkuUrl: String?
        get() = urls["resolvedProductBySku"]

    val productByCodeUrl: String?
        get() = urls["resolvedProductLookUp"]

    val telecashVaultItemsUrl: String?
        get() = urls["telecashVaultItems"]

    val activeShopsUrl: String?
        get() = urls["activeShops"]

    /**
     * Sets the shop used for receiving store specific prices and identification in the
     * payment process.
     */
    var checkedInShop: Shop? = null
        set(value) {
            val currentShopId = this.checkedInShop?.id ?: ""
            val newShopId = value?.id ?: ""
            if (currentShopId != newShopId) {
                field = value
                if (newShopId == "") {
                    Snabble.userPreferences.lastCheckedInShopId = null
                } else {
                    Snabble.userPreferences.lastCheckedInShopId = newShopId
                }
                events.updateShop(value)
                shoppingCart.updatePrices(false)
            }
        }

    private var texts: MutableMap<String, String> = mutableMapOf()

    private var encodedCodesJsonObject: JsonObject? = null

    private val updateListeners: MutableList<OnProjectUpdatedListener> = CopyOnWriteArrayList()

    /**
     * The internal storage directly used for various files stored by the snabble SDK that
     * are related to this project.
     */
    lateinit var internalStorageDirectory: File

    /**
     * OkHttpClient which wraps http calls to the snabble backend with valid tokens.
     *
     * Only use this http client when communicating with the snabble backend on a project level.
     */
    lateinit var okHttpClient: OkHttpClient

    private lateinit var shoppingCartStorage: ShoppingCartStorage

    /**
     * The users shopping cart
     */
    lateinit var shoppingCart: ShoppingCart

    /**
     * Provides a list of active coupons
     */
    lateinit var coupons: Coupons

    /**
     * The snabble checkout API.
     *
     * Uses a state machine to let ui display the current state of a checkout.
     */
    lateinit var checkout: Checkout

    /**
     * The primary product database of this project.
     */
    lateinit var productDatabase: ProductDatabase

    /**
     * Event logger which ships logging data to the snabble backend.
     */
    lateinit var events: Events

    /**
     * Provides access to images used by various ui components
     */
    lateinit var assets: Assets

    init {
        parse(jsonObject)
    }

    /**
     * Parse a json definition of a Project.
     */
    fun parse(jsonObject: JsonObject) {
        id = if (jsonObject.has("id")) {
            jsonObject["id"].asString
        } else {
            throw IllegalArgumentException("Project has no id")
        }

        name = jsonObject.getString("name", id)

        val brandId = jsonObject.getStringOpt("brandID", null)
        if (brandId != null) {
            brand = Snabble.brands[brandId]
        }

        val urls: MutableMap<String, String> = mutableMapOf()
        val links = jsonObject["links"].asJsonObject
        links.entrySet().forEach {
            urls[it.key] = Snabble.absoluteUrl(it.value.asJsonObject["href"].asString)
        }
        this.urls = urls

        tokensUrl = "${urls["tokens"]}?role=retailerApp"
        appUserUrl = "${Snabble.createAppUserUrl}?project=${id}"

        roundingMode = parseRoundingMode(jsonObject["roundingMode"])
        currency = Currency.getInstance(jsonObject.getStringOpt("currency", "EUR"))
        val locale = jsonObject.getStringOpt("locale", "de_DE")
        currencyLocale = try {
            LocaleUtils.toLocale(locale)
        } catch (e: IllegalArgumentException) {
            Locale.getDefault()
        }
        currencyFractionDigits = jsonObject.getIntOpt("decimalDigits", 2)

        priceFormatter = PriceFormatter(this)
        isCheckoutAvailable = jsonObject.getBooleanOpt("enableCheckout", true)

        if (jsonObject.has("qrCodeOffline")) {
            val encodedCodes = jsonObject["qrCodeOffline"]
            if (!encodedCodes.isJsonNull) {
                encodedCodesJsonObject = encodedCodes.asJsonObject
                encodedCodesOptions =
                    EncodedCodesOptions.fromJsonObject(this, encodedCodesJsonObject)
            }
        }

        val scanFormats = jsonObject.getStringListOpt("scanFormats", null)
        supportedBarcodeFormats = scanFormats?.mapNotNull { BarcodeFormat.parse(it) } ?: listOf(
            BarcodeFormat.EAN_8,
            BarcodeFormat.EAN_13,
            BarcodeFormat.CODE_128
        )

        val customerCards = jsonObject.getAsJsonObject("customerCards")
        val acceptedCustomerCards = customerCards.getStringListOpt("accepted", emptyList())
        val requiredCustomerCard = customerCards.getStringOpt("required", null)
        val customerCardInfos: MutableList<CustomerCardInfo> = ArrayList()
        acceptedCustomerCards?.filterNotNull()?.forEach {
            var required = false
            if (it == requiredCustomerCard) {
                required = true
            }
            val customerCardInfo = CustomerCardInfo(it, required)
            customerCardInfos.add(customerCardInfo)
        }

        this.customerCardInfos = customerCardInfos
        if (requiredCustomerCard != null) {
            requiredCustomerCardInfo = CustomerCardInfo(requiredCustomerCard, true)
        }

        paymentMethodDescriptors = jsonObject["paymentMethodDescriptors"]?.let {
            val typeToken = object : TypeToken<List<PaymentMethodDescriptor?>?>() {}.type
            val paymentMethodDescriptors = GsonHolder.get().fromJson<List<PaymentMethodDescriptor>>(it, typeToken)
            paymentMethodDescriptors.filter { desc ->
                PaymentMethod.fromString(desc.id) != null
            }
        } ?: emptyList()

        shops = if (jsonObject.has("shops")) {
            Shop.fromJson(jsonObject["shops"])?.toList() ?: emptyList()
        } else {
            emptyList()
        }

        if (jsonObject.has("company")) {
            company = GsonHolder.get().fromJson(jsonObject["company"], Company::class.java)
        }

        val codeTemplates = mutableListOf<CodeTemplate>()
        jsonObject["codeTemplates"]?.asJsonObject?.entrySet()?.forEach {
            try {
                val codeTemplate = CodeTemplate(it.key, it.value.asString)
                codeTemplates.add(codeTemplate)
            } catch (e: Exception) {
                Logger.e("Could not parse template %s: %s", it.key, e.message)
            }
        }

        val hasDefaultTemplate = codeTemplates.find { it.name == "default" } != null
        if (!hasDefaultTemplate) {
            codeTemplates.add(CodeTemplate("default", "{code:*}"))
        }
        this.codeTemplates = codeTemplates

        val priceOverrideTemplates: MutableList<PriceOverrideTemplate> = mutableListOf()
        jsonObject["priceOverrideCodes"]?.asJsonArray?.forEach {
            val priceOverride = it.asJsonObject
            try {
                val codeTemplate = CodeTemplate(
                    priceOverride["id"].asString,
                    priceOverride["template"].asString
                )
                var matchingTemplate: CodeTemplate? = null
                if (priceOverride.has("transmissionTemplate")) {
                    matchingTemplate = getCodeTemplate(priceOverride["transmissionTemplate"].asString)
                }
                val priceOverrideTemplate = PriceOverrideTemplate(
                    codeTemplate,
                    matchingTemplate,
                    priceOverride.getStringOpt("transmissionCode", null)
                )
                priceOverrideTemplates.add(priceOverrideTemplate)
            } catch (e: Exception) {
                Logger.e("Could not parse priceOverrideTemplate %s", e.message)
            }
        }
        this.priceOverrideTemplates = priceOverrideTemplates

        searchableTemplates = jsonObject.getStringListOpt("searchableTemplates", listOf("default"))
            ?.filterNotNull()
            ?: emptyList()

        if (jsonObject.has("checkoutLimits")) {
            val checkoutLimits = jsonObject.getAsJsonObject("checkoutLimits")
            maxCheckoutLimit = checkoutLimits.getIntOpt("checkoutNotAvailable", 0)
            maxOnlinePaymentLimit = checkoutLimits.getIntOpt("notAllMethodsAvailable", 0)
        }

        texts = mutableMapOf()

        if (jsonObject.has("texts")) {
            val textsElement = jsonObject["texts"]
            if (!textsElement.isJsonNull) {
                val textsJsonObject = jsonObject["texts"].asJsonObject
                textsJsonObject.entrySet().forEach {
                    texts[it.key] = it.value.asString
                }
            }
        }

        isDisplayingNetPrice = jsonObject.getBooleanOpt("displayNetPrice", false)

        var couponList = emptyList<Coupon>()
        try {
            if (jsonObject.has("coupons")) {
                val couponsJsonObject = jsonObject["coupons"]
                val couponsType = object : TypeToken<List<Coupon?>?>() {}.type
                couponList = GsonHolder.get().fromJson(couponsJsonObject, couponsType)
            }
        } catch (e: Exception) {
            Logger.e("Could not parse coupons")
        }

        internalStorageDirectory = File(Snabble.internalStorageDirectory, "$id/")

        okHttpClient = Snabble.okHttpClient
            .newBuilder()
            .addInterceptor(SnabbleAuthorizationInterceptor(this))
            .addInterceptor(AcceptedLanguageInterceptor())
            .build()

        shoppingCartStorage = ShoppingCartStorage(this)

        shoppingCart = shoppingCartStorage.shoppingCart

        checkout = Checkout(this, shoppingCart)

        productDatabase = ProductDatabase(this, shoppingCart, "$id.sqlite3", Snabble.config.generateSearchIndex)

        events = Events(this, shoppingCart)

        assets = Assets(this)

        googlePayHelper = paymentMethodDescriptors
            .map { it.paymentMethod }
            .firstOrNull { it == PaymentMethod.GOOGLE_PAY }
            ?.let {
                GooglePayHelper(this, instance.application)
            }

        coupons = Coupons(this)
        if (coupons.source.value !== CouponSource.Online) {
            coupons.setProjectCoupons(couponList)
        }
        coupons.update()

        notifyUpdate()
    }

    var googlePayHelper = paymentMethodDescriptors
        .map { it.paymentMethod }
        .firstOrNull { it == PaymentMethod.GOOGLE_PAY }
        ?.let {
            GooglePayHelper(this, instance.application)
        }

    /**
     * Causes hidden shops to be loaded asynchronously if config.loadActiveShops is set to true.
     *
     * Otherwise does nothing.
     */
    fun loadActiveShops(done: Runnable?) {
        if (Snabble.config.loadActiveShops) {
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
        } else {
            done?.run()
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

    /**
     * List of payment methods that should be available to the user.
     */
    val availablePaymentMethods: List<PaymentMethod>
        get() = paymentMethodDescriptors.map { it.paymentMethod }

    /**
     * The code template that should be used, when no code template is specified by a scannable code
     */
    val defaultCodeTemplate: CodeTemplate?
        get() = getCodeTemplate("default")


    fun getCodeTemplate(name: String): CodeTemplate? =
        codeTemplates.find { it.name == name }

    fun getTransformationTemplate(name: String): CodeTemplate? =
        priceOverrideTemplates.find { it.transmissionCodeTemplate?.name == name }?.codeTemplate

    @JvmOverloads
    fun getText(key: String, defaultValue: String? = null): String? {
        return texts[key] ?: return defaultValue
    }

    /**
     * Logs a event tagged with error to the snabble Backend.
     */
    fun logErrorEvent(format: String?, vararg args: Any?) {
        Logger.e(format, *args)
        events.logError(format, *args)
    }

    /**
     * Logs a event to the snabble Backend.
     */
    fun logEvent(format: String?, vararg args: Any?) {
        Logger.e(format, *args)
        events.log(format, *args)
    }

    private fun notifyUpdate() {
        updateListeners.forEach {
            it.onProjectUpdated(this)
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
}