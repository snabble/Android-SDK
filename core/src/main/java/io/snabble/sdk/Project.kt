package io.snabble.sdk

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.auth.SnabbleAuthorizationInterceptor
import io.snabble.sdk.checkout.Checkout
import io.snabble.sdk.codes.templates.CodeTemplate
import io.snabble.sdk.codes.templates.PriceOverrideTemplate
import io.snabble.sdk.coupons.Coupon
import io.snabble.sdk.coupons.CouponSource
import io.snabble.sdk.coupons.Coupons
import io.snabble.sdk.encodedcodes.EncodedCodesOptions
import io.snabble.sdk.events.Events
import io.snabble.sdk.googlepay.GooglePayHelper
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.ShoppingCartStorage
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import io.snabble.sdk.utils.SimpleJsonCallback
import io.snabble.sdk.utils.getBooleanOpt
import io.snabble.sdk.utils.getIntOpt
import io.snabble.sdk.utils.getString
import io.snabble.sdk.utils.getStringListOpt
import io.snabble.sdk.utils.getStringOpt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.lang3.LocaleUtils
import java.io.File
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A project contains configuration information and backend api urls needed for a
 * retailer.
 */
class Project internal constructor(jsonObject: JsonObject) {

    /**
     * The unique identifier of the Project
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

    /**
     * Object describing the company associated for this project.
     */
    var company: Company? = null
        private set

    /**
     * If multiple projects are linked to the same retailer, they can have a optional Brand
     * associated with them, so they can be grouped together. E.g. in the Payment Options Screen
     */
    var brand: Brand? = null
        private set

    /**
     * The roundingMode which should be used when doing offline calculations
     */
    var roundingMode: RoundingMode = RoundingMode.HALF_UP
        private set

    /**
     * The currency used to calculate and display prices
     */
    lateinit var currency: Currency
        private set

    /**
     * The locale in which this currency is used
     */
    lateinit var currencyLocale: Locale
        private set

    /**
     * The number of used currency fractions digits
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
     * (for example: Checkout using a QR-Code) should be encoded
     */
    var encodedCodesOptions: EncodedCodesOptions? = null
        private set

    /**
     * List of supported barcode formats used by this retailer. The scanner should restrict its
     * scanning to include only those formats
     */
    var supportedBarcodeFormats = emptyList<BarcodeFormat>()
        private set

    /**
     * Indicator if prices should be displayed using the net price instead of the gross price
     */
    var isDisplayingNetPrice = false
        private set

    /**
     * List of payment method descriptors indicating which
     * payment methods and providers are available in the Project
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
        private set

    /**
     * Returns the possible accepted cards and if a customer card is required
     */
    var customerCardInfo = emptyList<CustomerCardInfo>()
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
     * Sets the customer card number for user identification with the backend
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
    var codeTemplates = emptyList<CodeTemplate>()
        private set

    /**
     * List of code templates that are used when supplying an existing Product with a different
     * barcode which contains a reduced price
     */
    var priceOverrideTemplates = emptyList<PriceOverrideTemplate>()
        private set

    /**
     * List of code templates that are searchable using the barcode search functionality
     */
    var searchableTemplates = emptyList<String>()
        private set

    /**
     * A price formatter for formatting prices using the provided currency information.
     *
     * Also provides functions to display embedded prices of a scanned code.
     */
    lateinit var priceFormatter: PriceFormatter
        private set

    /**
     * A key-value map containing urls provided by the metadata.
     * All urls are absolute, even if the original metadata contained relative urls.
     */
    var urls = emptyMap<String, String>()
        private set

    /**
     * Url to retrieve authentication tokens
     */
    var tokensUrl: String? = null
        private set

    /**
     * Url to retrieve app user information
     */
    var appUserUrl: String? = null
        private set

    /**
     * Url to post events to the snabble Backend (e.g. cart updates)
     */
    val eventsUrl: String?
        get() = urls["appEvents"]

    /**
     * Url to download the product database
     */
    val appDbUrl: String?
        get() = urls["appdb"]

    /**
     * Url to start a checkout flow
     */
    val checkoutUrl: String?
        get() = urls["checkoutInfo"]

    /**
     * Url to retrieve image assets that change dynamically between retailers
     */
    val assetsUrl: String?
        get() = urls["assetsManifest"]

    /**
     * Url to retrieve products by sku
     */
    val productBySkuUrl: String?
        get() = urls["resolvedProductBySku"]

    /**
     * Url to retrieve products by barcode
     */
    val productByCodeUrl: String?
        get() = urls["resolvedProductLookUp"]

    @Deprecated(message = "Use url provieded in paymentMethodDescriptor")
    val telecashVaultItemsUrl: String?
        get() = urls["telecashVaultItems"]

    /**
     * Url to load shops that are not currently 'live'. Shops that are already live are already
     * included in the normal metadata json and do'nt need to be loaded afterwards
     */
    val activeShopsUrl: String?
        get() = urls["activeShops"]

    private var texts = mutableMapOf<String, String>()

    private var encodedCodesJsonObject: JsonObject? = null

    private val updateListeners = CopyOnWriteArrayList<OnProjectUpdatedListener>()

    /**
     * The internal storage directly used for various files stored by the snabble SDK that
     * are related to this project.
     */
    lateinit var internalStorageDirectory: File
        private set

    /**
     * OkHttpClient which wraps http calls to the snabble backend with valid tokens.
     *
     * Only use this http client when communicating with the snabble backend on a project level.
     */
    lateinit var okHttpClient: OkHttpClient
        private set

    private lateinit var shoppingCartStorage: ShoppingCartStorage
        private set

    private val _shoppingCart = MutableStateFlow(ShoppingCart())

    /**
     * Flow to observe the current users shopping cart
     */
    val shoppingCartFlow: StateFlow<ShoppingCart> = _shoppingCart.asStateFlow()

    /**
     * The users shopping cart
     */
    val shoppingCart: ShoppingCart
        get() = shoppingCartFlow.value

    /**
     * Provides a list of active coupons
     */
    lateinit var coupons: Coupons
        private set

    /**
     * The snabble checkout API.
     *
     * Uses a state machine to let ui display the current state of a checkout.
     */
    lateinit var checkout: Checkout
        private set

    /**
     * The primary product database of this project
     */
    lateinit var productDatabase: ProductDatabase
        private set

    /**
     * Event logger which ships logging data to the snabble backend
     */
    lateinit var events: Events
        private set

    /**
     * Provides access to images used by various ui components
     */
    lateinit var assets: Assets
        private set

    init {
        parse(jsonObject)
    }

    /**
     * Parse a json definition of a Project
     */
    fun parse(jsonObject: JsonObject) {
        id = requireNotNull(jsonObject["id"]?.asString) { "Project has no id" }
        name = jsonObject.getString("name", id)

        val brandId = jsonObject.getStringOpt("brandID", null)
        if (brandId != null) {
            brand = Snabble.brands[brandId]
        }

        val urls = mutableMapOf<String, String>()
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
        val customerCardInfo = mutableListOf<CustomerCardInfo>()
        acceptedCustomerCards?.filterNotNull()?.forEach {
            var required = false
            if (it == requiredCustomerCard) {
                required = true
            }
            val info = CustomerCardInfo(it, required)
            customerCardInfo.add(info)
        }

        this.customerCardInfo = customerCardInfo
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

        val codeTemplates = jsonObject["codeTemplates"]?.asJsonObject?.entrySet()?.map { (name, pattern) ->
            CodeTemplate(name, pattern.asString)
        }?.toMutableList() ?: mutableListOf()

        val hasDefaultTemplate = codeTemplates.any { it.name == "default" }
        if (!hasDefaultTemplate) {
            codeTemplates.add(CodeTemplate("default", "{code:*}"))
        }
        this.codeTemplates = codeTemplates

        val priceOverrideTemplates = mutableListOf<PriceOverrideTemplate>()
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
                textsJsonObject.entrySet().forEach { (key, value) ->
                    texts[key] = value.asString
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

        _shoppingCart.tryEmit(ShoppingCart(this))

        shoppingCartStorage = ShoppingCartStorage(this)

        checkout = Checkout(this, shoppingCartFlow.value)

        productDatabase = ProductDatabase(this, shoppingCartFlow.value, "$id.sqlite3", Snabble.config.generateSearchIndex)

        events = Events(this, shoppingCartFlow.value)

        assets = Assets(this)

        googlePayHelper = paymentMethodDescriptors
            .mapNotNull { it.paymentMethod }
            .firstOrNull { it == PaymentMethod.GOOGLE_PAY }
            ?.let {
                GooglePayHelper(this, Snabble.application)
            }

        coupons = Coupons(this)
        if (coupons.source.value !== CouponSource.Online) {
            coupons.setProjectCoupons(couponList)
        }
        coupons.update()

        notifyUpdate()
    }

    var googlePayHelper = paymentMethodDescriptors
        .mapNotNull { it.paymentMethod }
        .firstOrNull { it == PaymentMethod.GOOGLE_PAY }
        ?.let {
            GooglePayHelper(this, Snabble.application)
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

    private fun parseRoundingMode(jsonElement: JsonElement?) =
        when (jsonElement?.asString) {
            "up" -> RoundingMode.UP
            "down" -> RoundingMode.DOWN
            else -> RoundingMode.HALF_UP
        }

    /**
     * List of payment methods that should be available to the user
     */
    val availablePaymentMethods
        get() = paymentMethodDescriptors.mapNotNull { it.paymentMethod }

    /**
     * The code template that should be used, when no code template is specified by a scannable code
     */
    val defaultCodeTemplate
        get() = getCodeTemplate("default")

    /**
     * Get a code template by its name
     */
    fun getCodeTemplate(name: String) =
        codeTemplates.find { it.name == name }

    /**
     * Get a code transformation template by its name
     */
    fun getTransformationTemplate(name: String?) =
        name?.let {
            priceOverrideTemplates.find { it.transmissionCodeTemplate?.name == name }?.codeTemplate
        }

    /**
     * Get text included in the metadata
     */
    @JvmOverloads
    fun getText(key: String, defaultValue: String? = null) =
        texts[key] ?: defaultValue

    /**
     * Logs a event tagged with error to the snabble Backend
     */
    fun logErrorEvent(format: String?, vararg args: Any?) {
        Logger.e(format, *args)
        events.logError(format, *args)
    }

    /**
     * Logs a event to the snabble Backend
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

    fun interface OnProjectUpdatedListener {

        fun onProjectUpdated(project: Project?)
    }
}
