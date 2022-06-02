package io.snabble.setup

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.DefaultAndroidSourceSet
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.lang.IllegalStateException

open class SnabblePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("snabble", SnabbleExtension::class.java, project)
        project.pluginManager.withPlugin("com.android.application") {
            val assetsDir = File(
                project.buildDir,
                "intermediates/snabble/assets"
            )
            val downloadTask = project.tasks.register(
                "downloadSnabbleMetadata",
                DownloadTask::class.java
            ) {
                it.appId.set(extension.appId ?: throw IllegalStateException("You must define the app id in order to download the manifest"))
                it.outputDir.set(assetsDir)
            }

            val manifestFile = File(
                project.buildDir,
                "intermediates/snabble/manifest/AndroidManifest.xml"
            )
            val manifestTask = project.tasks.register(
                "generateSnabbleManifest",
                GenerateMetaTagsTask::class.java
            ) {
                it.appId.set(extension.appId ?: throw IllegalStateException("You must define the app id"))
                it.secret.set(extension.secret ?: throw IllegalStateException("You must define the secret"))
                it.endpoint.set(extension.endpoint)
                it.manifestFile = manifestFile
            }

            project.extensions.getByType(BaseExtension::class.java).forEachVariant { variant ->
                val sourceSet = DefaultAndroidSourceSet("snabble", project, false)
                variant.sourceSets += sourceSet
                if (variant.buildType.isDebuggable && extension.prefetchMetaData) {
                    project.tasks.getByName("generate${variant.capitalizedName()}Resources").dependsOn += downloadTask
                    sourceSet.assets.srcDirs(assetsDir)
                }
                project.tasks.getByName("generate${variant.capitalizedName()}Resources").dependsOn += manifestTask
                sourceSet.manifest.srcFile(manifestFile)
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
    var endpoint: String? = null
    var prefetchMetaData = false
}

@CacheableTask
abstract class DownloadTask : DefaultTask() {
    @get:Input
    abstract val appId: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun download() {
        val outputDirFile = outputDir.asFile.get()
        if (outputDirFile.exists() && !outputDirFile.deleteRecursively()) {
            logger.warn("Failed to clear directory for snabble setup")
        }
        println("TODO download metadata for ${appId.get()} to $outputDirFile; User-Agent: ${UserAgentInterceptor().userAgent}")
    }
}
@CacheableTask
abstract class GenerateMetaTagsTask : DefaultTask() {
    @get:Input
    abstract val appId: Property<String>
    @get:Input
    abstract val secret: Property<String>
    @get:Input
    abstract val endpoint: Property<String?>

    @get:OutputFile
    abstract var manifestFile: File

    @TaskAction
    fun download() {
        val manifestPath = manifestFile.parentFile
        if (manifestPath.exists() && manifestPath.deleteRecursively()) {
            logger.warn("Failed to clear directory for snabble setup")
        }
        println("TODO generate metadata for ${appId} in $manifestFile")
    }
}