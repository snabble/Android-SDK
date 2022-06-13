package io.snabble.sdk

import android.content.Context
import com.google.gson.*
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import okhttp3.Interceptor
import java.io.File
import java.lang.Exception
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * Configuration of the snabble SDK.
 */
data class Config (
    /**
     * The endpoint url of the snabble backend. For example "https://api.snabble.io" for the Production environment.
     */
    @JvmField
    var endpointBaseUrl: String = Environment.PRODUCTION.baseUrl,

    /**
     * The project identifier, which is used in the communication with the backend.
     */
    @JvmField
    var appId: String? = null,

    /**
     * The secret needed for Totp token generation
     */
    @JvmField
    var secret: String? = null,

    /**
     * Relative path from the assets folder which points to a bundled file which contains the metadata.
     *
     * This file gets initially used to initialize the SDK before network requests are made,
     * or be able to use the sdk in the case of no network connection.
     *
     * Optional. If no file is specified every time the SDK is initialized we wait for a network response
     * from the backend.
     *
     * It is HIGHLY recommended to provide bundled metadata to allow the sdk to function
     * without having a network connection.
     */
    @JvmField
    var bundledMetadataAssetPath: String? = null,

    /**
     * If set to true, creates an full text index to support searching in the product database
     * using findByName or searchByName.
     *
     * Note that this increases setup time of the ProductDatabase, and it may not be
     * immediately available offline.
     */
    @JvmField
    var generateSearchIndex: Boolean = false,

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
    var maxProductDatabaseAge: Long = TimeUnit.HOURS.toMillis(1),

    /**
     * The time that the shopping cart is allowed to be alive after the last modification.
     *
     * The time is specified in milliseconds.
     *
     * The default value is 4 hours.
     */
    @JvmField
    var maxShoppingCartAge: Long  = TimeUnit.HOURS.toMillis(4),

    /** If set to true, disables certificate pinning. Not recommended for production!  */
    @JvmField
    var disableCertificatePinning: Boolean = false,

    /** SQL queries that will get executed in order on the product database. */
    @JvmField
    var initialSQL: List<String> = emptyList(),

    /** Vibrate while adding a product to the cart, by default false. */
    @JvmField
    var vibrateToConfirmCartFilled: Boolean = false,

    /**
     * Set to true, to load shops that are marked as pre launch
     * and are not part of the original metadata in the backend
     * (for example for testing shops in production before a go-live)
     */
    @JvmField
    var loadActiveShops: Boolean = false,

    /**
     * The radius in which the CheckInManager tries to check in a shop.
     *
     * In meters.
     */
    @JvmField
    var checkInRadius: Float = 500.0f,

    /**
     * The radius in which the CheckInManager tries to stay in a shop, if already in it.
     * If outside of this radius and the lastSeenThreshold, you will be checked out.
     */
    @JvmField
    var checkOutRadius: Float = 1000.0f,

    /**
     * The time in milliseconds which we keep you checked in at a shop.
     *
     * The timer will be refreshed while you are still inside the shop
     * and only begins to run if you are not inside the checkOutRadius anymore.
     */
    @JvmField
    var lastSeenThreshold: Long = TimeUnit.MINUTES.toMillis(15),

    /**
     * Network interceptor used for all calls made by the SDK.
     */
    @JvmField
    var networkInterceptor: Interceptor? = null,

    /**
     * Set to true if you want to control when the product database gets updated, otherwise
     * the product database gets updated when checking in and if checked in when the app resumes
     */
    @JvmField
    var manualProductDatabaseUpdates: Boolean = false,
) {
    fun save(context: Context) {
        val file = File(context.filesDir, "snabble/${fileName}/")
        val json = gson.toJson(this)
        Dispatch.io {
            try {
                file.writeText(json)
            } catch (e: Throwable) {
                Logger.e("Failed to save config to ${file.path}: ${e.message}")
            }
        }
    }

    companion object {
        const val fileName = "config.json"

        private val gson = GsonHolder.get()
            .newBuilder()
            .registerTypeAdapter(Interceptor::class.java, InterceptorSerializer)
            .create()

        fun restore(context: Context): Config? {
            val file = File(context.filesDir, "snabble/${fileName}/")
            return try {
                val text = file.readText()
                val config = gson.fromJson(text, Config::class.java)
                config
            } catch (e: Throwable) {
                Logger.e("Failed to load config to ${file.path}: ${e.message}")
                null
            }
        }
    }
}

object InterceptorSerializer : JsonSerializer<Interceptor?>, JsonDeserializer<Interceptor?> {
    override fun serialize(src: Interceptor?,
                           typeOfSrc: Type?,
                           context: JsonSerializationContext?
    ): JsonElement {
        val cls = src?.javaClass?.name
        return if (cls != null) {
            JsonPrimitive(cls)
        } else {
            JsonNull.INSTANCE
        }
    }

    override fun deserialize(json: JsonElement?,
                             typeOfT: Type?,
                             context: JsonDeserializationContext?
    ): Interceptor? {
        val cls = json?.asString
        return if (cls != null) {
            val clazz = Class.forName(cls)
            clazz.newInstance() as Interceptor
        } else {
            null
        }
    }
}