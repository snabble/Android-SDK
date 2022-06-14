package io.snabble.setup

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.DefaultAndroidSourceSet
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.lang.IllegalStateException

open class SnabbleExtension(private val project: Project) {
    val environments: Map<Environment, BuildEnvironment> = mutableMapOf()
    private val baseDir = File(
        project.buildDir,
        "intermediates/snabble"
    )

    private fun getEnvironment(environment: Environment) =
        (environments as MutableMap<Environment, BuildEnvironment>).getOrPut(environment) {
            BuildEnvironment(environment)
        }

    fun production(environment: Action<BuildEnvironment>) {
        environment.execute(getEnvironment(Environment.Production))
        createTasks(Environment.Production)
    }

    fun staging(environment: Action<BuildEnvironment>) {
        environment.execute(getEnvironment(Environment.Staging))
        createTasks(Environment.Staging)
    }

    fun testing(environment: Action<BuildEnvironment>) {
        environment.execute(getEnvironment(Environment.Testing))
        createTasks(Environment.Testing)
    }

    private fun createTasks(environment: Environment) {
        val extension = environments[environment]!!
        val downloadTask = project.tasks.register(
            "downloadSnabble${environment}Metadata",
            DownloadTask::class.java
        ) {
            val appId = extension.appId ?: throw IllegalStateException("You must define the app id in order to download the manifest")
            val app = project.extensions.getByType(BaseExtension::class.java) as AppExtension
            val appVersion = app.defaultConfig.versionName ?: throw IllegalStateException("No app version number detected")
            it.url.set("${environment.baseUrl}/metadata/app/$appId/android/$appVersion")
            val path = extension.bundledMetadataAssetPath ?: "snabble/metadata$environment.json"
            it.outputFile = File(baseDir, "assets/$path")
        }

        val generateConfigTask = project.tasks.register(
            "generateSnabble${environment}Config",
            GenerateSnabbleConfigTask::class.java
        ) {
            it.appId.set(extension.appId ?: throw IllegalStateException("You must define the app id"))
            // environment secrets
            it.secret.set(extension.secret ?: throw IllegalStateException("You must define the secret"))
            it.endpointBaseUrl.set(extension.endpointBaseUrl)
            it.bundledMetadataAssetPath.set(extension.bundledMetadataAssetPath)
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
            it.configFile = File(baseDir, "assets/snabble/config$environment.properties")
        }

        project.extensions.getByType(BaseExtension::class.java).forEachVariant { variant ->
            // generate config{Environment}.properties
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