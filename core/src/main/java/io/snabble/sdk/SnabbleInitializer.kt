package io.snabble.sdk

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.startup.Initializer

/**
 * Initializer for the snabble SDK using androidx.startup.
 */
class SnabbleInitializer : Initializer<Snabble> {
    override fun create(context: Context): Snabble {
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
                manualProductDatabaseUpdates = getBoolean("snabble_manual_product_database_updates", manualProductDatabaseUpdates)
            }

            Snabble.setup(app, config)
            return Snabble
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}