package io.snabble.setup

import org.gradle.api.Project
import java.util.concurrent.TimeUnit

open class SnabbleExtension(project: Project) {
    var appId: String? = null
    var secret: String? = null
    var endpointBaseUrl: String? = null
    var prefetchMetaData = false
    var bundledMetadataAssetPath: String? = null
        get() = field ?: if (prefetchMetaData) "snabble/metadata.json" else null
    var generateSearchIndex: Boolean = false
    var maxProductDatabaseAge: Long = TimeUnit.HOURS.toMillis(1)
    var maxShoppingCartAge: Long = TimeUnit.HOURS.toMillis(4)
    var disableCertificatePinning: Boolean = false
    var vibrateToConfirmCartFilled: Boolean = false
    var loadActiveShops: Boolean = false
    var checkInRadius: Float = 500.0f
    var checkOutRadius: Float = 1000.0f
    var lastSeenThreshold: Long = TimeUnit.MINUTES.toMillis(15)
    var networkInterceptor: String? = null
    var manualProductDatabaseUpdates: Boolean = false
}