package io.snabble.sdk

import io.snabble.sdk.Project
import io.snabble.sdk.auth.TokenRegistry
import io.snabble.sdk.Receipts
import io.snabble.sdk.Users
import io.snabble.sdk.MetadataDownloader
import io.snabble.sdk.UserPreferences
import io.snabble.sdk.payment.PaymentCredentialsStore
import io.snabble.sdk.checkin.CheckInLocationManager
import io.snabble.sdk.checkin.CheckInManager
import io.snabble.sdk.TermsOfService
import okhttp3.OkHttpClient
import android.app.Activity
import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.snabble.sdk.InitializationState
import io.snabble.sdk.Snabble.SetupCompletionListener
import io.snabble.sdk.utils.Logger.ErrorEventHandler
import io.snabble.sdk.utils.Logger.LogEventHandler
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.snabble.sdk.OkHttpClientFactory
import io.snabble.sdk.auth.AppUser
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import com.google.gson.JsonObject
import com.google.gson.JsonElement
import com.google.gson.JsonArray
import kotlin.Throws
import io.snabble.sdk.Snabble.SnabbleException
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.util.Base64
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.ref.WeakReference
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

class Snabble private constructor() {
    lateinit var okHttpClient: OkHttpClient
        private set

    lateinit var tokenRegistry: TokenRegistry
        private set
    
    lateinit var projects: List<Project>
        private set

    var brands: Map<String, Brand>? = null
        private set

    lateinit var receipts: Receipts
        private set

    lateinit var users: Users
        private set

    lateinit var application: Application
        private set

    lateinit var userPreferences: UserPreferences
        private set

    lateinit var paymentCredentialsStore: PaymentCredentialsStore
        private set

    lateinit var checkInLocationManager: CheckInLocationManager
        private set

    lateinit var checkInManager: CheckInManager
        private set

    lateinit var internalStorageDirectory: File
        private set

    lateinit var termsOfService: TermsOfService
        private set

    lateinit var config: Config
        private set

    var versionName: String? = null
        private set

    var environment: Environment? = null
        private set
    
    var paymentCertificates: List<X509Certificate>? = null
        private set

    var metadataUrl: String? = null
        private set
    
    var receiptsUrl: String? = null
        get() {
            val url = receiptsUrl
            val appUser = userPreferences.appUser
            return if (appUser != null && url != null) {
                url.replace("{appUserID}", appUser.id)
            } else {
                null
            }
        }
        private set

    var usersUrl: String? = null
        private set

    var consentUrl: String? = null
        private set

    var telecashSecretUrl: String? = null
        private set

    var telecashPreAuthUrl: String? = null
        private set

    var paydirektAuthUrl: String? = null
        private set

    var createAppUserUrl: String? = null
        private set

    var initializationState = MutableLiveData(InitializationState.NONE)
        private set

    private var currentActivity: WeakReference<Activity>? = null

    private lateinit var metadataDownloader: MetadataDownloader

    private val onMetaDataUpdateListeners: MutableList<OnMetadataUpdateListener> = CopyOnWriteArrayList()

    fun setup(app: Application, config: Config, setupCompletionListener: SetupCompletionListener) {
        initializationState.postValue(InitializationState.INITIALIZING)
        
        application = app
        this.config = config
        
        Logger.setErrorEventHandler { message, args -> Events.logErrorEvent(null, message, *args) }
        Logger.setLogEventHandler { message, args -> Events.logErrorEvent(null, message, *args) }
        
        if (config.appId == null || config.secret == null) {
            setupCompletionListener.onError(Error.CONFIG_PARAMETER_MISSING)
            return
        }
        
        var version = config.versionName
        if (version == null) {
            version = try {
                val pInfo = app.packageManager.getPackageInfo(app.packageName, 0)
                if (pInfo != null && pInfo.versionName != null) {
                    pInfo.versionName.toLowerCase(Locale.ROOT).replace(" ", "")
                } else {
                    "1.0"
                }
            } catch (e: PackageManager.NameNotFoundException) {
                "1.0"
            }
        }
        versionName = version
        
        internalStorageDirectory = File(application.filesDir, "snabble/" + config.appId + "/")
        internalStorageDirectory.mkdirs()
        
        okHttpClient = OkHttpClientFactory.createOkHttpClient(app)
        userPreferences = UserPreferences(app)
        tokenRegistry = TokenRegistry(okHttpClient, userPreferences, config.appId, config.secret)
        receipts = Receipts()
        users = Users(userPreferences)
        brands = Collections.unmodifiableMap(HashMap())
        projects = Collections.unmodifiableList(ArrayList())

        if (config.endpointBaseUrl == null) {
            config.endpointBaseUrl = Environment.PRODUCTION.baseUrl
        } else if (!config.endpointBaseUrl!!.startsWith("http://") && !config.endpointBaseUrl!!.startsWith("https://")) {
            config.endpointBaseUrl = "https://" + config.endpointBaseUrl
        }
        
        environment = Environment.getEnvironmentByUrl(config.endpointBaseUrl)
        metadataUrl = absoluteUrl("/metadata/app/" + config.appId + "/android/" + version)
        paymentCredentialsStore = PaymentCredentialsStore()

        checkInLocationManager = CheckInLocationManager(application)
        checkInManager = CheckInManager(this,
            checkInLocationManager,
            config.checkInRadius,
            config.checkOutRadius,
            config.lastSeenThreshold
        )

        metadataDownloader = MetadataDownloader(okHttpClient, config.bundledMetadataAssetPath)
        
        if (config.bundledMetadataAssetPath != null) {
            readMetadata()
            setupCompletionListener.onReady()
            initializationState.postValue(InitializationState.INITIALIZED)
            if (config.loadActiveShops) {
                loadActiveShops()
            }
        } else {
            metadataDownloader.loadAsync(object : Downloader.Callback() {
                override fun onDataLoaded(wasStillValid: Boolean) {
                    readMetadata()
                    val appUser = userPreferences.appUser
                    if (appUser == null && projects.size > 0) {
                        val token = tokenRegistry.getToken(projects.get(0))
                        if (token == null) {
                            setupCompletionListener.onError(Error.CONNECTION_TIMEOUT)
                            initializationState.postValue(InitializationState.ERROR)
                            return
                        }
                    }
                    setupCompletionListener.onReady()
                    initializationState.postValue(InitializationState.INITIALIZED)
                    if (config.loadActiveShops) {
                        loadActiveShops()
                    }
                }

                override fun onError() {
                    if (metadataDownloader.hasData()) {
                        readMetadata()
                        setupCompletionListener.onReady()
                        initializationState.postValue(InitializationState.INITIALIZED)
                    } else {
                        setupCompletionListener.onError(Error.CONNECTION_TIMEOUT)
                        initializationState.postValue(InitializationState.ERROR)
                    }
                }
            })
        }
        
        app.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        registerNetworkCallback(app)
    }

    private fun registerNetworkCallback(app: Application) {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build(),
            networkCallback)
    }

    fun getInitializationState(): LiveData<InitializationState> {
        return initializationState
    }

    /**
     * Returns true when the SDK is not compatible with the backend anymore and the app should
     * notify the user that it will not function anymore.
     */
    val isOutdatedSDK: Boolean
        get() {
            val jsonObject = additionalMetadata
            return if (jsonObject != null) {
                JsonUtils.getBooleanOpt(jsonObject, "kill", false)
            } else false
        }

    /** Returns additional metadata that may be provided for apps unrelated to the SDK  */
    val additionalMetadata: JsonObject?
        get() {
            val jsonObject = metadataDownloader.jsonObject
            val jsonElement = jsonObject["metadata"]
            return jsonElement?.asJsonObject
        }

    @Synchronized
    private fun readMetadata() {
        val jsonObject = metadataDownloader.jsonObject
        if (jsonObject != null) {
            createAppUserUrl = getUrl(jsonObject, "createAppUser")
            telecashSecretUrl = getUrl(jsonObject, "telecashSecret")
            telecashPreAuthUrl = getUrl(jsonObject, "telecashPreauth")
            paydirektAuthUrl = getUrl(jsonObject, "paydirektCustomerAuthorization")
            
            if (jsonObject.has("brands")) {
                parseBrands(jsonObject)
            }
            
            if (jsonObject.has("projects")) {
                parseProjects(jsonObject)
            }
            
            if (jsonObject.has("gatewayCertificates")) {
                parsePaymentCertificates(jsonObject)
            }
            
            receiptsUrl = getUrl(jsonObject, "appUserOrders")
            usersUrl = getUrl(jsonObject, "appUser")
            consentUrl = getUrl(jsonObject, "consents")
            if (jsonObject.has("terms")) {
                termsOfService =
                    GsonHolder.get().fromJson(jsonObject["terms"], TermsOfService::class.java)
            }
        }
        
        paymentCredentialsStore.init(application, environment)
        users.postPendingConsents()
        checkInManager.update()
    }

    private fun getUrl(jsonObject: JsonObject, urlName: String): String? {
        return try {
            absoluteUrl(jsonObject["links"].asJsonObject[urlName].asJsonObject["href"].asString)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseBrands(jsonObject: JsonObject) {
        val jsonBrands = GsonHolder.get().fromJson(jsonObject["brands"], Array<Brand>::class.java)
        val map = HashMap<String, Brand>()
        for (brand in jsonBrands) {
            map[brand.id] = brand
        }
        brands = Collections.unmodifiableMap(map)
    }

    private fun parseProjects(jsonObject: JsonObject) {
        val jsonArray = jsonObject["projects"].asJsonArray
        val newProjects: MutableList<Project> = ArrayList()
        for (i in 0 until jsonArray.size()) {
            val jsonProject = jsonArray[i].asJsonObject

            // first try to find an already existing project and update it, so that
            // the object reference can be stored somewhere and still be up to date
            var updated = false
            if (jsonProject.has("id")) {
                for (p in projects) {
                    if (p.id == jsonProject["id"].asString) {
                        try {
                            p.parse(jsonProject)
                            newProjects.add(p)
                        } catch (e: IllegalArgumentException) {
                            // malformed project, do nothing
                        }
                        updated = true
                        break
                    }
                }

                // if it does not exist, add it
                if (!updated) {
                    try {
                        val project = Project(jsonProject)
                        newProjects.add(project)
                    } catch (e: IllegalArgumentException) {
                        Logger.d(e.message)
                        // malformed project, do nothing
                    }
                }
            }
        }
        
        projects = Collections.unmodifiableList(newProjects)
    }

    private fun parsePaymentCertificates(jsonObject: JsonObject) {
        val certificates: MutableList<X509Certificate> = ArrayList()
        val certs = jsonObject["gatewayCertificates"].asJsonArray
        for (i in 0 until certs.size()) {
            val jsonElement = certs[i]
            if (jsonElement.isJsonObject) {
                val cert = jsonElement.asJsonObject
                val value = cert["value"]
                if (value != null) {
                    val bytes = Base64.decode(value.asString, Base64.DEFAULT)
                    val `is`: InputStream = ByteArrayInputStream(bytes)
                    try {
                        val certificateFactory = CertificateFactory.getInstance("X.509")
                        val certificate =
                            certificateFactory.generateCertificate(`is`) as X509Certificate
                        certificates.add(certificate)
                    } catch (e: CertificateException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        
        paymentCertificates = Collections.unmodifiableList(certificates)
    }

    fun absoluteUrl(url: String): String {
        return if (url.startsWith("http")) {
            url
        } else {
            endpointBaseUrl + url
        }
    }

    val endpointBaseUrl: String?
        get() = config.endpointBaseUrl

    fun getProjectById(projectId: String): Project? {
        for (project in projects) {
            if (project.id == projectId) {
                return project
            }
        }
        return null
    }

    /**
     * The blocking version of [.setup]
     *
     *
     * Blocks until every initialization is completed, that includes waiting for necessary
     * network calls if bundled data is not provided.
     *
     *
     * If all needed bundled data is provided (See [Config]), initialization requires
     * no network calls.
     *
     * @throws SnabbleException If an error occurs while initializing the sdk.
     */
    @Throws(SnabbleException::class)
    fun setupBlocking(app: Application, config: Config) {
        val countDownLatch = CountDownLatch(1)
        val snabbleError = arrayOfNulls<Error>(1)
        setup(app, config, object : SetupCompletionListener {
            override fun onReady() {
                countDownLatch.countDown()
            }

            override fun onError(error: Error?) {
                snabbleError[0] = error
                countDownLatch.countDown()
            }
        })
        try {
            countDownLatch.await()
        } catch (e: InterruptedException) {
            throw SnabbleException(Error.UNSPECIFIED_ERROR)
        }
        if (snabbleError[0] != null) {
            throw SnabbleException(snabbleError[0])
        }
    }

    private fun updateMetadata() {
        metadataDownloader.url = metadataUrl
        metadataDownloader.loadAsync(object : Downloader.Callback() {
            override fun onDataLoaded(wasStillValid: Boolean) {
                if (!wasStillValid) {
                    readMetadata()
                    notifyMetadataUpdated()
                }
                if (config.loadActiveShops) {
                    loadActiveShops()
                }
            }
        })
    }

    private fun loadActiveShops() {
        for (project in projects) {
            project.loadActiveShops { notifyMetadataUpdated() }
        }
    }

    private fun notifyMetadataUpdated() {
        for (listener in onMetaDataUpdateListeners) {
            listener.onMetaDataUpdated()
        }
    }

    private fun checkCartTimeouts() {
        for (project in projects) {
            project.shoppingCart.checkForTimeout()
        }
    }

    private fun processPendingCheckouts() {
        for (project in projects) {
            project.checkout.processPendingCheckouts()
        }
    }

    /**
     * Adds a listener that gets called every time the metadata updates
     */
    fun addOnMetadataUpdateListener(onMetaDataUpdateListener: OnMetadataUpdateListener) {
        onMetaDataUpdateListeners.add(onMetaDataUpdateListener)
    }

    /**
     * Removes an already added listener
     */
    fun removeOnMetadataUpdateListener(onMetaDataUpdateListener: OnMetadataUpdateListener) {
        onMetaDataUpdateListeners.remove(onMetaDataUpdateListener)
    }

    interface OnMetadataUpdateListener {
        fun onMetaDataUpdated()
    }

    private val activityLifecycleCallbacks: ActivityLifecycleCallbacks =
        object : SimpleActivityLifecycleCallbacks() {
            override fun onActivityStarted(activity: Activity) {
                currentActivity?.clear()
                currentActivity = null
                currentActivity = WeakReference(activity)
                updateMetadata()
                checkCartTimeouts()
                processPendingCheckouts()
            }

            override fun onActivityStopped(activity: Activity) {
                if (currentActivity?.get() === activity) {
                    currentActivity?.clear()
                    currentActivity = null
                }
            }
        }
    private val networkCallback: NetworkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            onConnectionStateChanged(true)
        }

        override fun onLost(network: Network) {
            onConnectionStateChanged(false)
        }

        override fun onUnavailable() {
            onConnectionStateChanged(false)
        }
    }

    private fun onConnectionStateChanged(isConnected: Boolean) {
        if (isConnected) {
            processPendingCheckouts()
        }
        for (project in projects) {
            project.shoppingCart.updatePrices(false)
        }
    }

    /**
     * Unique identifier, different over device installations
     */
    val clientId: String
        get() = userPreferences.clientId

    interface SetupCompletionListener {
        fun onReady()
        fun onError(error: Error?)
    }

    class SnabbleException internal constructor(val error: Error?) : Exception() {
        override fun toString(): String {
            return "SnabbleException{" +
                    "error=" + error +
                    '}'
        }
    }

    enum class Error {
        UNSPECIFIED_ERROR, CONFIG_PARAMETER_MISSING, CONNECTION_TIMEOUT, INVALID_METADATA_FORMAT, INTERNAL_STORAGE_FULL
    }

    class Config {
        /**
         * The endpoint url of the snabble backend. For example "snabble.io" for the Production environment.
         *
         * If null points to the Production Environment
         */
        @JvmField
        var endpointBaseUrl: String? = null

        /**
         * Relative path from the assets folder which points to a bundled file which contains the metadata
         *
         *
         * This file gets initially used to initialize the sdk before network requests are made,
         * or be able to use the sdk in the case of no network connection.
         *
         *
         * Optional. If no file is specified every time the sdk is initialized we wait for a network response
         * from the backend.
         *
         *
         * It is HIGHLY recommended to provide bundled metadata to allow the sdk to function
         * without having a network connection.
         */
        @JvmField
        var bundledMetadataAssetPath: String? = null

        /**
         * The project identifier, which is used in the communication with the backend.
         */
        @JvmField
        var appId: String? = null

        /**
         * The secret needed for Totp token generation
         */
        @JvmField
        var secret: String? = null

        /**
         * Optional. Used to override the versionName appended to the metadata url.
         *
         *
         * Defaults to the versionName in the app package.
         *
         *
         * Must be in the format %d.%d
         */
        @JvmField
        var versionName: String? = null

        /**
         * Optional SSLSocketFactory that gets used for HTTPS requests.
         *
         *
         * Requires also x509TrustManager to be set.
         */
        @JvmField
        var sslSocketFactory: SSLSocketFactory? = null

        /**
         * Optional X509TrustManager that gets used for HTTPS requests.
         *
         *
         * Requires also sslSocketFactory to be set.
         */
        @JvmField
        var x509TrustManager: X509TrustManager? = null

        /**
         * If set to true, creates an full text index to support searching in the product database
         * using findByName or searchByName.
         *
         *
         * Note that this increases setup time of the ProductDatabase, and it may not be
         * immediately available offline.
         */
        @JvmField
        var generateSearchIndex = false

        /**
         * The time that the database is allowed to be out of date. After the specified time in
         * milliseconds the database only uses online requests for asynchronous requests.
         *
         * Successfully calling [ProductDatabase.update] resets the timer.
         *
         * The time is specified in milliseconds.
         *
         * The default value is 1 hour.
         */
        @JvmField
        var maxProductDatabaseAge = TimeUnit.HOURS.toMillis(1)

        /**
         * The time that the shopping cart is allowed to be alive after the last modification.
         *
         * The time is specified in milliseconds.
         *
         * The default value is 4 hours.
         */
        @JvmField
        var maxShoppingCartAge = TimeUnit.HOURS.toMillis(4)

        /** If set to true, disables certificate pinning  */
        @JvmField
        var disableCertificatePinning = false

        /** SQL queries that will get executed in order on the product database  */
        @JvmField
        var initialSQL: Array<String>? = null

        /** Vibrate while adding a product to the cart, by default false  */
        @JvmField
        var vibrateToConfirmCartFilled = false

        /** Set to true, to load shops that are marked as pre launch
         * and are not part of the original metadata in the backend
         * (for example for testing shops in production before a go-live)  */
        @JvmField
        var loadActiveShops = false

        /**
         * The radius in which the CheckInManager tries to check in a shop.
         *
         * In meters.
         */
        @JvmField
        var checkInRadius = 500.0f

        /**
         * The radius in which the CheckInManager tries to stay in a shop, if already in it.
         * If outside of this radius and the lastSeenThreshold, you will be checked out.
         */
        @JvmField
        var checkOutRadius = 1000.0f

        /**
         * The time in milliseconds which we keep you checked in at a shop.
         *
         * The timer will be refreshed while you are still inside the shop
         * and only begins to run if you are not inside the checkOutRadius anymore.
         */
        @JvmField
        var lastSeenThreshold = TimeUnit.MINUTES.toMillis(15)
    }

    companion object {
        private val _instance = Snabble()

        @JvmStatic
        fun getInstance() : Snabble {
            return _instance
        }

        @JvmStatic
        val version: String
            get() = BuildConfig.VERSION_NAME

        /**
         * Enables debug logging.
         */
        @JvmStatic
        fun setDebugLoggingEnabled(enabled: Boolean) {
            Logger.setEnabled(enabled)
        }
    }
}