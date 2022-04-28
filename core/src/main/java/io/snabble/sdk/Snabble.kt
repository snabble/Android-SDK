package io.snabble.sdk

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonObject
import io.snabble.sdk.auth.TokenRegistry
import io.snabble.sdk.checkin.CheckInLocationManager
import io.snabble.sdk.checkin.CheckInManager
import io.snabble.sdk.payment.PaymentCredentialsStore
import io.snabble.sdk.utils.*
import okhttp3.OkHttpClient
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.lang.ref.WeakReference
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The heart of the snabble SDK. Initialization and object access is provided via this facade.
 */
object Snabble {
    /**
     * Retrieve the global instance of snabble.
     *
     * For Kotlin use the Snabble object directly.
     */
    @JvmStatic
    val instance: Snabble
        get() = this

    /**
     * The version of the SDK.
     */
    @JvmStatic
    val version: String
        get() = BuildConfig.VERSION_NAME

    /**
     * OkHttpClient for general use.
     *
     * Does not provide automatic authentication, use the OkHttpClient
     * provided in the individual projects for this.
     */
    lateinit var okHttpClient: OkHttpClient
        private set

    /**
     * Registry to retrieve and generate access tokens for backend api access.
     */
    lateinit var tokenRegistry: TokenRegistry
        private set

    /**
     * The list of available projects.
     *
     * For single retailer's this is usually size of 1. If you have multiple regions there could be
     * multiple Projects even for a single retailer.
     */
    lateinit var projects: List<Project>
        private set

    /**
     * Map of available brands.
     */
    lateinit var brands: Map<String, Brand>
        private set

    /**
     * API-Wrapper to retrieve receipts.
     */
    lateinit var receipts: Receipts
        private set

    /**
     * Access to user specific information, e.g. User-Consent / Unique-ID
     */
    lateinit var users: Users
        private set

    /**
     * The main Android application
     */
    lateinit var application: Application
        private set

    /**
     * Local persisted user preferences
     */
    lateinit var userPreferences: UserPreferences
        private set

    /**
     * Add and retrieve local encrypted payment credentials
     */
    lateinit var paymentCredentialsStore: PaymentCredentialsStore
        private set

    /**
     * Location manager for use with the check in manager
     */
    lateinit var checkInLocationManager: CheckInLocationManager
        private set

    /**
     * Geo-fencing based check in manager. Use for automatically detecting if you are in a shop.
     *
     * Calling functions may require location permission
     */
    lateinit var checkInManager: CheckInManager
        private set

    /**
     * The internal storage directory in which snabble stores files.
     */
    lateinit var internalStorageDirectory: File
        private set

    /**
     * Retrieve the link for our terms of service.
     */
    var termsOfService: TermsOfService? = null
        private set

    /**
     * The config provided after calling Snabble.setup
     */
    lateinit var config: Config
        private set

    /**
     * snabble SDK version name.
     */
    var versionName: String? = null
        private set

    /**
     * The environment the SDK is using.
     *
     * Available environments are.
     *
     * Testing (https://api.snabble-testing.io)
     * Staging (https://api.snabble-staging.io)
     * Production (https://api.snabble.io)
     */
    var environment: Environment? = null
        private set

    /**
     * List of certificates to match against user encrypted payment certificates. If those are
     * not valid anymore, our backend is not able to decode the encrypted payment credentials
     * anymore.
     *
     * This would usually only happen in a severe security incident and is not something
     * you actively need to consider.
     */
    var paymentCertificates: List<X509Certificate>? = null
        private set

    /**
     * The url where all the properties come from.
     */
    var metadataUrl: String? = null
        private set

    /**
     * Url to retrieve receipts of the app user.
     */
    var receiptsUrl: String? = null
        get() = field?.let { url ->
            userPreferences.appUser?.let { appUser ->
                url.replace("{appUserID}", appUser.id)
            } ?: url
        }
        private set

    /**
     * Url to retrieve user related information.
     */
    var usersUrl: String? = null
        private set

    /**
     * Url to retrieve consent status of the user.
     */
    var consentUrl: String? = null
        private set

    /**
     * Url for generating telecash web form authentication challenges
     */
    var telecashSecretUrl: String? = null
        private set

    /**
     * Url for generating telecash web form authentication challenges
     */
    var telecashPreAuthUrl: String? = null
        private set

    /**
     * Url for generating payone web form authentication challenges
     */
    var paydirektAuthUrl: String? = null
        private set

    /**
     * Url to create a new app user
     */
    var createAppUserUrl: String? = null
        private set

    private val mutableInitializationState = MutableLiveData<InitializationState>()

    /**
     * The current initialization state of the SDK. Fragments observe this state and only
     * if the SDK is initialized will get displayed to the user.
     */
    val initializationState: LiveData<InitializationState> = mutableInitializationState

    /**
     * Weak reference to the current activity.
     */
    var currentActivity: WeakReference<Activity>? = null

    /**
     * Sets the shop used for receiving store specific prices and identification in the
     * payment process.
     */
    var checkedInShop: Shop? = null
        set(value) {
            val currentShopId = this.checkedInShop?.id.orEmpty()
            val newShopId = value?.id.orEmpty()
            if (currentShopId != newShopId) {
                field = value
                if (newShopId == "") {
                    userPreferences.lastCheckedInShopId = null
                    checkedInProject.value = null
                } else {
                    userPreferences.lastCheckedInShopId = newShopId

                    for (project in projects) {
                        if (project.shops.find { it.id == newShopId } != null) {
                            project.events.updateShop(value)
                            project.shoppingCart.updatePrices(false)
                            checkedInProject.value = project
                            break
                        }
                    }
                }
            }
        }

    var checkedInProject = MutableAccessibleLiveData<Project?>(null)

    /**
     * Unique identifier, different over device installations
     */
    val clientId: String?
        get() = userPreferences.clientId

    private lateinit var metadataDownloader: MetadataDownloader

    private val onMetaDataUpdateListeners: MutableList<OnMetadataUpdateListener> = CopyOnWriteArrayList()

    private val isInitializing = AtomicBoolean(false)

    /**
     * Setup the snabble SDK.
     *
     * First-time initialization is asynchronously. If the snabble SDK was already initialized before
     * the callback will be invoked after decoding the persisted metadata and requires no network connection.
     *
     * If you also want your first time usage to allow for no network scenarios, you must include the
     * raw metadata from our backend to bundledMetadataAssetPath in the assets folder.
     *
     * @param app Your main android application
     * @param config Config provided. Minimal required fields are appId and secret.
     * @param setupCompletionListener Completion listener that gets called when the SDK is ready.
     */
    @JvmOverloads
    fun setup(app: Application, config: Config, setupCompletionListener: SetupCompletionListener? = null) {
        if (isInitializing.get()) {
            return
        }

        isInitializing.set(true)
        mutableInitializationState.postValue(InitializationState.INITIALIZING)
        
        application = app
        this.config = config
        
        Logger.setErrorEventHandler { message, args -> Events.logErrorEvent(null, message, *args) }
        Logger.setLogEventHandler { message, args -> Events.logErrorEvent(null, message, *args) }

        if (!config.endpointBaseUrl.startsWith("http://")
         && !config.endpointBaseUrl.startsWith("https://")) {
            setupCompletionListener?.onError(Error.CONFIG_ERROR)
            return
        }

        var version = config.versionName
        if (version == null) {
            version = try {
                val pInfo = app.packageManager.getPackageInfo(app.packageName, 0)
                pInfo?.versionName?.lowercase(Locale.ROOT)?.replace(" ", "") ?: "1.0"
            } catch (e: PackageManager.NameNotFoundException) {
                "1.0"
            }
        }
        versionName = version
        
        internalStorageDirectory = File(application.filesDir, "snabble/${config.appId}/")
        internalStorageDirectory.mkdirs()

        okHttpClient = OkHttpClientFactory.createOkHttpClient(app)
        userPreferences = UserPreferences(app)
        tokenRegistry = TokenRegistry(okHttpClient, userPreferences, config.appId, config.secret)
        receipts = Receipts()
        users = Users(userPreferences)
        brands = Collections.unmodifiableMap(emptyMap())
        projects = Collections.unmodifiableList(emptyList())
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
            dispatchOnReady(setupCompletionListener)
        } else {
            metadataDownloader.loadAsync(object : Downloader.Callback() {
                override fun onDataLoaded(wasStillValid: Boolean) {
                    dispatchOnReady(setupCompletionListener)
                }

                override fun onError() {
                    if (metadataDownloader.hasData()) {
                        dispatchOnReady(setupCompletionListener)
                    } else {
                        dispatchError(setupCompletionListener)
                    }
                }
            })
        }
        
        app.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        registerNetworkCallback(app)
    }

    private fun dispatchOnReady(setupCompletionListener: SetupCompletionListener?) {
        Dispatch.background {
            readMetadata()
            val appUser = userPreferences.appUser
            if (appUser == null && projects.isNotEmpty()) {
                val token = tokenRegistry.getToken(projects[0])
                if (token == null) {
                    isInitializing.set(false)
                    mutableInitializationState.postValue(InitializationState.ERROR)

                    Dispatch.mainThread {
                        setupCompletionListener?.onError(Error.CONNECTION_TIMEOUT)
                    }
                    return@background
                }
            }

            isInitializing.set(false)
            mutableInitializationState.postValue(InitializationState.INITIALIZED)
            Dispatch.mainThread {
                setupCompletionListener?.onReady()
            }
            if (config.loadActiveShops) {
                loadActiveShops()
            }
        }
    }

    private fun dispatchError(setupCompletionListener: SetupCompletionListener?) {
        isInitializing.set(false)
        mutableInitializationState.postValue(InitializationState.ERROR)

        Dispatch.mainThread {
            setupCompletionListener?.onError(Error.CONNECTION_TIMEOUT)
        }
    }

    /**
     * The blocking version of [.setup]
     *
     * Blocks until every initialization is completed, that includes waiting for necessary
     * network calls if bundled data is not provided.
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

    private fun registerNetworkCallback(app: Application) {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build(),
            networkCallback)
    }

    /**
     * Returns true when the SDK is not compatible with the backend anymore and the app should
     * notify the user that it will not function anymore.
     */
    val isOutdatedSDK: Boolean
        get() = additionalMetadata?.let { json ->
            json.getBooleanOpt("kill", false)
        } ?: false

    /** Returns additional metadata that may be provided for apps unrelated to the SDK  */
    val additionalMetadata: JsonObject?
        get() {
            val jsonObject = metadataDownloader.jsonObject
            val jsonElement = jsonObject["metadata"]
            return jsonElement?.asJsonObject
        }

    @Synchronized
    private fun readMetadata() {
        metadataDownloader.jsonObject?.let { jsonObject ->
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

        restoreCheckedInShop()
        paymentCredentialsStore.init(application, environment)
        users.postPendingConsents()
        checkInManager.update()
    }

    private fun restoreCheckedInShop() {
        val lastCheckedInShopId = userPreferences.lastCheckedInShopId
        if (lastCheckedInShopId != null) {
            for (project in projects) {
                val shop = project.shops.find { shop ->
                    shop.id == lastCheckedInShopId
                }
                if (shop != null) {
                    Logger.d("Restoring last checked in shop " + shop.id + ", " + shop.name)
                    checkedInShop = shop
                    break;
                }
            }
        }
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
        brands = jsonBrands.map { it.id to it }.toMap()
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
        certs.forEach { jsonElement ->
            if (jsonElement.isJsonObject) {
                val cert = jsonElement.asJsonObject
                val value = cert["value"]
                if (value != null) {
                    val bytes = Base64.decode(value.asString, Base64.DEFAULT)
                    val inputStream: InputStream = ByteArrayInputStream(bytes)
                    try {
                        val certificateFactory = CertificateFactory.getInstance("X.509")
                        val certificate = certificateFactory.generateCertificate(inputStream) as X509Certificate
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

    val endpointBaseUrl: String
        get() = config.endpointBaseUrl

    fun getProjectById(projectId: String?): Project? {
        return projects.firstOrNull { it.id == projectId }
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
        projects.forEach { project ->
            project.loadActiveShops { notifyMetadataUpdated() }
        }
    }

    private fun checkCartTimeouts() {
        projects.forEach { project ->
            project.shoppingCart.checkForTimeout()
        }
    }

    private fun processPendingCheckouts() {
        projects.forEach { project ->
            project.checkout.processPendingCheckouts()
        }
    }

    private fun notifyMetadataUpdated() {
        onMetaDataUpdateListeners.forEach { listener ->
            listener.onMetaDataUpdated()
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

    fun interface OnMetadataUpdateListener {
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
        UNSPECIFIED_ERROR,
        CONFIG_ERROR,
        CONNECTION_TIMEOUT
    }

    /**
     * Enables debug logging.
     */
    @JvmStatic
    fun setDebugLoggingEnabled(enabled: Boolean) {
        Logger.setEnabled(enabled)
    }
}