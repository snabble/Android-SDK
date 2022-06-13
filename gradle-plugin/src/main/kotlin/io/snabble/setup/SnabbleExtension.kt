package io.snabble.setup

import org.gradle.api.Project
import java.util.concurrent.TimeUnit

/**
 * Configuration of the snabble gradle plugin.
 */
open class SnabbleExtension(project: Project) {
    /**
     * The project identifier, which is used in the communication with the backend.
     */
    var appId: String? = null

    /**
     * The secret needed for Totp token generation
     */
    var secret: String? = null

    /**
     * Optional. The endpoint url of the snabble backend. For example "https://api.snabble.io" for the Production
     * environment.
     * Can be `null` to use the Production environment.
     */
    var endpointBaseUrl: String? = null

    /**
     * Optional. Set to `true` to download the metadata to [bundledMetadataAssetPath]. Is always `true` when
     * [prefetchMetaDataForDebugBuilds] is set to `true`.
     */
    var prefetchMetaData = false
        get() = prefetchMetaDataForDebugBuilds || field

    /**
     * Optional. Set to `true` to download the metadata for each build similar to [prefetchMetaData]. The metadata are
     * stored to [bundledMetadataAssetPath].
     */
    var prefetchMetaDataForDebugBuilds = false

    /**
     * Relative path from the assets folder which points to a bundled file which contains the metadata.
     *
     * This file gets initially used to initialize the SDK before network requests are made,
     * or be able to use the sdk in the case of no network connection.
     *
     * Optional. If no file is specified `"snabble/metadata.json"` will be used.
     */
    var bundledMetadataAssetPath: String? = null
        get() = field ?: if (prefetchMetaData) "snabble/metadata.json" else null

    /**
     * If set to true, creates an full text index to support searching in the product database
     * using findByName or searchByName.
     *
     * Note that this increases setup time of the ProductDatabase, and it may not be
     * immediately available offline.
     */
    var generateSearchIndex: Boolean = false

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
    var maxProductDatabaseAge: Long = TimeUnit.HOURS.toMillis(1)

    /**
     * The time that the shopping cart is allowed to be alive after the last modification.
     *
     * The time is specified in milliseconds.
     *
     * The default value is 4 hours.
     */
    var maxShoppingCartAge: Long = TimeUnit.HOURS.toMillis(4)

    /** If set to true, disables certificate pinning. Not recommended for production! */
    var disableCertificatePinning: Boolean = false

    /** Vibrate while adding a product to the cart, by default false. */
    var vibrateToConfirmCartFilled: Boolean = false

    /**
     * Set to true, to load shops that are marked as pre launch
     * and are not part of the original metadata in the backend
     * (for example for testing shops in production before a go-live)
     */
    var loadActiveShops: Boolean = false

    /**
     * The radius in which the CheckInManager tries to check in a shop.
     *
     * In meters.
     */
    var checkInRadius: Float = 500.0f

    /**
     * The radius in which the CheckInManager tries to stay in a shop, if already in it.
     * If outside of this radius and the lastSeenThreshold, you will be checked out.
     */
    var checkOutRadius: Float = 1000.0f

    /**
     * The time in milliseconds which we keep you checked in at a shop.
     *
     * The timer will be refreshed while you are still inside the shop
     * and only begins to run if you are not inside the checkOutRadius anymore.
     */
    var lastSeenThreshold: Long = TimeUnit.MINUTES.toMillis(15)

    /**
     * Network interceptor used for all calls made by the SDK.
     */
    var networkInterceptor: String? = null

    /**
     * Set to true if you want to control when the product database gets updated, otherwise
     * the product database gets updated when checking in and if checked in when the app resumes
     */
    var manualProductDatabaseUpdates: Boolean = false
}