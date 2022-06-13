package io.snabble.setup

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.DefaultAndroidSourceSet
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit

open class SnabblePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("snabble", SnabbleExtension::class.java, project)
        project.pluginManager.withPlugin("com.android.application") { androidPlugin ->
            val baseDir = File(
                project.buildDir,
                "intermediates/snabble"
            )

            val downloadTask = project.tasks.register(
                "downloadSnabbleMetadata",
                DownloadTask::class.java
            ) {
                val appId = extension.appId ?: throw IllegalStateException("You must define the app id in order to download the manifest")
                val app = project.extensions.getByType(BaseExtension::class.java) as AppExtension
                val appVersion = app.defaultConfig.versionName ?: throw IllegalStateException("No app version number detected")
                it.url.set("https://api.snabble.io/metadata/app/$appId/android/$appVersion")
                it.outputFile = File(baseDir, "assets/snabble/metadata.json")
            }

            val generateConfigTask = project.tasks.register(
                "generateSnabbleConfig",
                GenerateSnabbleConfigTask::class.java
            ) {
                it.appId.set(extension.appId ?: throw IllegalStateException("You must define the app id"))
                it.secret.set(extension.secret ?: throw IllegalStateException("You must define the secret"))
                it.endpointBaseUrl.set(extension.endpointBaseUrl)
                it.bundledMetadataAssetPath.set(extension.bundledMetadataAssetPath)
                it.versionName.set(extension.versionName)
                it.generateSearchIndex.set(extension.generateSearchIndex)
                it.maxProductDatabaseAge.set(extension.maxProductDatabaseAge)
                it.maxShoppingCartAge.set(extension.maxShoppingCartAge)
                it.disableCertificatePinning.set(extension.disableCertificatePinning)
                it.vibrateToConfirmCartFilled.set(extension.vibrateToConfirmCartFilled)
                it.loadActiveShops.set(extension.loadActiveShops)
                it.checkInRadius.set(extension.checkInRadius)
                it.checkOutRadius.set(extension.checkOutRadius)
                it.lastSeenThreshold.set(extension.lastSeenThreshold)
                it.networkInterceptor.set(extension.networkInterceptor)
                it.manualProductDatabaseUpdates.set(extension.manualProductDatabaseUpdates)
                it.configFile = File(baseDir, "assets/snabble/config.properties")
            }

            project.extensions.getByType(BaseExtension::class.java).forEachVariant { variant ->
                // generate config.properties
                project.tasks.getByName("generate${variant.capitalizedName()}Assets").dependsOn += generateConfigTask
                // prefetch meta data
                if (!variant.buildType.isDebuggable && extension.prefetchMetaData) {
                    project.tasks.getByName("generate${variant.capitalizedName()}Assets").dependsOn += downloadTask
                }
                // inject assets directory
                variant.sourceSets.forEach { sourceSet ->
                    (sourceSet as DefaultAndroidSourceSet).assets.srcDir(project.file(File(baseDir, "assets")))
                }
            }

            // inject maven repositories
            project.repositories.maven { repo ->
                repo.name = "Snabble Maven Repository"
                repo.setUrl("https://raw.githubusercontent.com/snabble/maven-repository/releases")
                repo.content {
                    it.includeGroup("io.snabble.sdk")
                    it.includeModule("com.paypal.android", "risk-component")
                    it.includeModule("com.samsung.android.sdk", "samsungpay")
                    it.includeModule("ch.datatrans", "android-sdk")
                    it.includeModule("ch.twint.payment", "twint-sdk-android")
                }
            }
            project.repositories.maven { repo ->
                repo.name = "Datatrans Maven Repository"
                repo.setUrl("https://datatrans.jfrog.io/artifactory/mobile-sdk/")
            }
        }
    }

    @Suppress("DEPRECATION") // For BaseVariant should be replaced in later studio versions
    private fun BaseExtension.forEachVariant(action: (BaseVariant) -> Unit) {
        when (this) {
            is AppExtension -> applicationVariants.all(action)
            is LibraryExtension -> {
                libraryVariants.all(action)
            }
            else -> throw GradleException(
                "deeplink builder plugin must be used with android app, library or feature plugin"
            )
        }
    }

    @Suppress("DEPRECATION") // For BaseVariant should be replaced in later studio versions
    private fun BaseVariant.capitalizedName() = Character.toUpperCase(name[0]) + name.substring(1)
}

open class SnabbleExtension(project: Project) {
    var appId: String? = null
    var secret: String? = null
    var endpointBaseUrl: String? = null
    var prefetchMetaData = false
    var bundledMetadataAssetPath: String? = null
    var versionName: String? = null
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