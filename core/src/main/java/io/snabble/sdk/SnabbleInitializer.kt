package io.snabble.sdk

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.startup.Initializer
import io.snabble.sdk.utils.Logger
import okhttp3.Interceptor

/**
 * Initializer for the snabble SDK using androidx.startup
 */
class SnabbleInitializer : Initializer<Snabble> {
    override fun create(context: Context): Snabble {
        val app = context.applicationContext as Application
        var hasPropertiesFile = false
        var hasMetaData = false
        context.resources.assets.list("snabble/")?.forEach {
            hasPropertiesFile = hasPropertiesFile || it.endsWith("config.properties")
            hasMetaData = hasMetaData || it.endsWith("metadata.json")
        }

        fun Properties.getBoolean(key: String, default: Boolean) =
            getProperty(key).toBooleanStrictOrNull() ?: default
        fun Properties.getLong(key: String, default: Long) =
            getProperty(key).toLongOrDefault(default)
        fun Properties.getFloat(key: String, default: Float) =
            getProperty(key).toFloatOrNull() ?: default
        if (hasPropertiesFile) {
            val properties = Properties()
            properties.load(context.resources.assets.open("snabble/config.properties"))
            val config = Config().apply {
                appId = properties.getProperty("appId")
                endpointBaseUrl = properties.getProperty("endpointBaseUrl")
                secret = properties.getProperty("secret")
                bundledMetadataAssetPath = properties.getProperty("bundledMetadataAssetPath")
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
                        Class.forName(properties.getProperty("networkInterceptor", null))?.newInstance() as Interceptor?
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

        val applicationInfo = app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
        with(applicationInfo.metaData) {
            if (getBoolean("snabble_auto_initialization_disabled")) {
                return Snabble
            }

            val config = Config().apply {
                appId = getString("snabble_app_id", appId)
                endpointBaseUrl = getString("snabble_endpoint_baseurl", endpointBaseUrl)
                secret = getString("snabble_secret", secret)
                bundledMetadataAssetPath = getString("snabble_bundled_metadata_asset_path", bundledMetadataAssetPath)
                versionName = getString("snabble_version_name", versionName)
                generateSearchIndex = getBoolean("snabble_generate_search_index", generateSearchIndex)
                maxProductDatabaseAge = getLong("snabble_max_product_database_age", maxProductDatabaseAge)
                maxShoppingCartAge = getLong("snabble_max_shopping_cart_age", maxShoppingCartAge)
                disableCertificatePinning = getBoolean("snabble_disable_certificate_pinning")
                initialSQL = getStringArrayList("snabble_initial_sql") ?: initialSQL
                vibrateToConfirmCartFilled = getBoolean("snabble_vibrate_to_confirm_cart_filled", vibrateToConfirmCartFilled)
                loadActiveShops = getBoolean("snabble_load_active_shops", loadActiveShops)
                checkInRadius = getFloat("snabble_check_in_radius", checkInRadius)
                checkOutRadius = getFloat("snabble_check_out_radius", checkOutRadius)
                lastSeenThreshold = getLong("snabble_last_seen_threshold", lastSeenThreshold)
                networkInterceptor =
                    try {
                        Class.forName(getString("snabble_network_interceptor", null))?.newInstance() as Interceptor?
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

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}