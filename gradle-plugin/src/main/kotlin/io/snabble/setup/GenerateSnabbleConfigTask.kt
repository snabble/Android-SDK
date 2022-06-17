package io.snabble.setup

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

/**
 * Generated the properties file for the SDK.
 */
@CacheableTask
abstract class GenerateSnabbleConfigTask : DefaultTask() {
    @get:Input
    abstract val appId: Property<String>
    @get:Input
    abstract val secret: Property<String>
    @get:Input
    abstract val endpointBaseUrl: Property<String?>
    @get:Input
    @get:Optional
    abstract val bundledMetadataAssetPath: Property<String?>
    @get:Input
    @get:Optional
    abstract val generateSearchIndex: Property<Boolean?>
    @get:Input
    @get:Optional
    abstract val maxProductDatabaseAge: Property<Long?>
    @get:Input
    @get:Optional
    abstract val maxShoppingCartAge: Property<Long?>
    @get:Input
    @get:Optional
    abstract val disableCertificatePinning: Property<Boolean?>
    @get:Input
    @get:Optional
    abstract val vibrateToConfirmCartFilled: Property<Boolean?>
    @get:Input
    @get:Optional
    abstract val loadActiveShops: Property<Boolean?>
    @get:Input
    @get:Optional
    abstract val checkInRadius: Property<Float?>
    @get:Input
    @get:Optional
    abstract val checkOutRadius: Property<Float?>
    @get:Input
    @get:Optional
    abstract val lastSeenThreshold: Property<Long?>
    @get:Input
    @get:Optional
    abstract val networkInterceptor: Property<String?>
    @get:Input
    @get:Optional
    abstract val manualProductDatabaseUpdates: Property<Boolean?>

    /**
     * The path of the config file.
     */
    @get:OutputFile
    abstract var configFile: File

    /**
     * Write all the input values in a properties file on the path of [configFile].
     */
    @TaskAction
    fun generateManifest() {
        if (endpointBaseUrl.get() == Environment.Production.baseUrl && disableCertificatePinning.orNull == true) {
            project.logger.warn("Certificate pinning disabled on production environment")
        }
        val properties = mapOf(
            "appId" to appId.get(),
            "secret" to secret.get(),
            "endpointBaseUrl" to endpointBaseUrl.get(),
            "bundledMetadataAssetPath" to bundledMetadataAssetPath.orNull,
            "generateSearchIndex" to generateSearchIndex.orNull,
            "maxProductDatabaseAge" to maxProductDatabaseAge.orNull,
            "maxShoppingCartAge" to maxShoppingCartAge.orNull,
            "disableCertificatePinning" to disableCertificatePinning.orNull,
            "vibrateToConfirmCartFilled" to vibrateToConfirmCartFilled.orNull,
            "loadActiveShops" to loadActiveShops.orNull,
            "checkInRadius" to checkInRadius.orNull,
            "checkOutRadius" to checkOutRadius.orNull,
            "lastSeenThreshold" to lastSeenThreshold.orNull,
            "networkInterceptor" to networkInterceptor.orNull,
            "manualProductDatabaseUpdates" to manualProductDatabaseUpdates.orNull,
        )
        val config = properties.entries
            .filter { (_, value) -> value != null }
            .joinToString("\n") { (key, value) -> "$key=$value" }
        configFile.writeText(config)
    }
}