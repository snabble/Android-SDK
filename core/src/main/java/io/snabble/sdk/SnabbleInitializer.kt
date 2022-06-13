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
        var hasPropertiesFile = false
        var hasMetaData = false
        context.resources.assets.list("snabble/")?.forEach {
            hasPropertiesFile = hasPropertiesFile || it.endsWith("config.properties")
            hasMetaData = hasMetaData || it.endsWith("metadata.json")
        }
        println("### hasPropertiesFile=$hasPropertiesFile hasMetaData=$hasMetaData")

        val app = context.applicationContext as Application
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