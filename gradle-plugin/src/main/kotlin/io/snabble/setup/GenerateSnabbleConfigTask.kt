package io.snabble.setup

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
abstract class GenerateSnabbleConfigTask : DefaultTask() {
    @get:Input
    abstract val appId: Property<String>
    @get:Input
    abstract val secret: Property<String>
    @get:Input
    @get:Optional
    abstract val endpointBaseUrl: Property<String?>
    @get:Input
    @get:Optional
    abstract val bundledMetadataAssetPath: Property<String?>
    @get:Input
    @get:Optional
    abstract val versionName: Property<String?>
    @get:Input
    abstract val generateSearchIndex: Property<Boolean>
    @get:Input
    abstract val maxProductDatabaseAge: Property<Long>
    @get:Input
    abstract val maxShoppingCartAge: Property<Long>
    @get:Input
    abstract val disableCertificatePinning: Property<Boolean>
    @get:Input
    @get:Optional
    abstract val initialSQL: ListProperty<String>
    @get:Input
    abstract val vibrateToConfirmCartFilled: Property<Boolean>
    @get:Input
    abstract val loadActiveShops: Property<Boolean>
    @get:Input
    abstract val checkInRadius: Property<Float>
    @get:Input
    abstract val checkOutRadius: Property<Float>
    @get:Input
    abstract val lastSeenThreshold: Property<Long>
    @get:Input
    @get:Optional
    abstract val networkInterceptor: Property<String?>
    @get:Input
    abstract val manualProductDatabaseUpdates: Property<Boolean>

    @get:OutputFile
    abstract var configFile: File

    @TaskAction
    fun generateManifest() {
        val properties = mapOf(
            "appId" to appId.orNull,
            "secret" to secret.orNull,
            "endpointBaseUrl" to endpointBaseUrl.orNull,
            "bundledMetadataAssetPath" to bundledMetadataAssetPath.orNull,
            "versionName" to versionName.orNull,
            "generateSearchIndex" to generateSearchIndex.orNull,
            "maxProductDatabaseAge" to maxProductDatabaseAge.orNull,
            "maxShoppingCartAge" to maxShoppingCartAge.orNull,
            "disableCertificatePinning" to disableCertificatePinning.orNull,
            "initialSQL" to initialSQL.orNull,
            "vibrateToConfirmCartFilled" to vibrateToConfirmCartFilled.orNull,
            "loadActiveShops" to loadActiveShops.orNull,
            "checkInRadius" to checkInRadius.orNull,
            "checkOutRadius" to checkOutRadius.orNull,
            "lastSeenThreshold" to lastSeenThreshold.orNull,
            "networkInterceptor" to networkInterceptor.orNull,
            "manualProductDatabaseUpdates" to manualProductDatabaseUpdates.orNull,
        )
        val config = properties.entries
            .filterNot { (_, value) ->
                value == null || (value as? List<Any?>)?.isEmpty() == true
            }
            .joinToString("\n") { (key, value) -> "$key=$value" }
        configFile.writeText(config)
    }
}