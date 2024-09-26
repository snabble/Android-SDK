package io.snabble.sdk

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Resources
import androidx.startup.Initializer
import io.snabble.sdk.extensions.getApplicationInfoCompat
import io.snabble.sdk.utils.Logger
import okhttp3.Interceptor
import java.util.Properties

/**
 * Initializer for the snabble SDK using androidx.startup
 */
class SnabbleInitializer : Initializer<Snabble> {

    override fun create(context: Context): Snabble {
        val app = context.applicationContext as Application

        // load properties created by the gradle plugin
        val env = UserPreferences(context).environment.name.lowercase()
        val resId = context.resources.getIdentifier("snabble_${env}_config", "raw", app.packageName)
        if (resId != Resources.ID_NULL) {
            val properties = Properties()
            properties.load(context.resources.openRawResource(resId))
            val config = Config().apply {
                appId = properties.getProperty("appId")
                endpointBaseUrl = properties.getProperty("endpointBaseUrl") ?: endpointBaseUrl
                secret = properties.getProperty("secret")
                val assetPath = properties.getProperty("bundledMetadataAssetPath")?.let { path ->
                    if (context.resources.assets.list(path)?.isEmpty() != false) {
                        null
                    } else {
                        path
                    }
                }
                bundledMetadataAssetPath = assetPath
                bundledMetadataRawResId = context.resources.getIdentifier("snabble_${env}_metadata", "raw", app.packageName)
                generateSearchIndex = properties.getBoolean("generateSearchIndex", generateSearchIndex)
                maxProductDatabaseAge = properties.getLong("maxProductDatabaseAge", maxProductDatabaseAge)
                maxShoppingCartAge = properties.getLong("maxShoppingCartAge", maxShoppingCartAge)
                disableCertificatePinning = properties.getBoolean("disableCertificatePinning", disableCertificatePinning)
                vibrateToConfirmCartFilled = properties.getBoolean("vibrateToConfirmCartFilled", vibrateToConfirmCartFilled)
                loadActiveShops = properties.getBoolean("loadActiveShops", loadActiveShops)
                checkInRadius = properties.getFloat("checkInRadius", checkInRadius)
                checkOutRadius = properties.getFloat("checkOutRadius", checkOutRadius)
                lastSeenThreshold = properties.getLong("lastSeenThreshold", lastSeenThreshold)
                networkInterceptor =
                    try {
                        Class.forName(properties.getProperty("networkInterceptor", null))
                            ?.getDeclaredConstructor()
                            ?.newInstance() as Interceptor?
                    } catch (e: Throwable) {
                        Logger.w("Could not instantiate network interceptor", e.message)
                        null
                    }
                manualProductDatabaseUpdates = properties.getBoolean("manualProductDatabaseUpdates", manualProductDatabaseUpdates)
            }

            if (config.appId == null || config.secret == null) {
                throw IllegalStateException("Please file a bug report with our build.gradle file. This state should not be possible.")
            } else {
                Snabble.setup(app, config)
                return Snabble
            }
        }

        val applicationInfo: ApplicationInfo = app.packageManager.getApplicationInfoCompat(app.packageName)

        with(applicationInfo.metaData) {
            if (getBoolean("snabble_auto_initialization_disabled")) {
                return Snabble
            }

            val config = Config().apply {
                appId = getString("snabble_app_id", appId)
                endpointBaseUrl = getString("snabble_endpoint_baseurl", endpointBaseUrl)
                secret = getString("snabble_secret", secret)
                bundledMetadataAssetPath = getString("snabble_bundled_metadata_asset_path", bundledMetadataAssetPath)
                generateSearchIndex = getBoolean("snabble_generate_search_index", generateSearchIndex)
                maxProductDatabaseAge = getLong("snabble_max_product_database_age", maxProductDatabaseAge)
                maxShoppingCartAge = getLong("snabble_max_shopping_cart_age", maxShoppingCartAge)
                disableCertificatePinning = getBoolean("snabble_disable_certificate_pinning")
                vibrateToConfirmCartFilled = getBoolean("snabble_vibrate_to_confirm_cart_filled", vibrateToConfirmCartFilled)
                loadActiveShops = getBoolean("snabble_load_active_shops", loadActiveShops)
                checkInRadius = getFloat("snabble_check_in_radius", checkInRadius)
                checkOutRadius = getFloat("snabble_check_out_radius", checkOutRadius)
                lastSeenThreshold = getLong("snabble_last_seen_threshold", lastSeenThreshold)
                networkInterceptor =
                    try {
                        Class.forName(getString("snabble_network_interceptor", null))
                            ?.getDeclaredConstructor()
                            ?.newInstance() as Interceptor?
                    } catch (e: Throwable) {
                        Logger.w("Could not instantiate network interceptor", e.message)
                        null
                    }
                manualProductDatabaseUpdates = getBoolean("snabble_manual_product_database_updates", manualProductDatabaseUpdates)
            }

            if (config.appId == null || config.secret == null) {
                Logger.w("To initialize the SDK either set 'snabble_app_id' and 'snabble_secret' or use Snabble.setup. " +
                        "To disable this warning set 'snabble_auto_initialization_disabled' to true.")
            } else {
                Snabble.setup(app, config)
            }

            return Snabble
        }
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
