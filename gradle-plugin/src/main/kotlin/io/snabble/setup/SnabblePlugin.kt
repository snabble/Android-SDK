package io.snabble.setup

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.lang.IllegalStateException

open class SnabblePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("snabble", SnabbleExtension::class.java, project)
        project.pluginManager.withPlugin("com.android.application") { androidPlugin ->
            val baseDir = File(
                project.buildDir,
                "intermediates/snabble"
            )
            val generateAarTask = project.tasks.register(
                "generateSnabbleAar",
                GenerateAarTask::class.java
            ) {
                it.sourceDir = File(baseDir, "src")
                it.aarFile = File(baseDir, "SnabbleSetup.aar")
            }

            val downloadTask = project.tasks.register(
                "downloadSnabbleMetadata",
                DownloadTask::class.java
            ) {
                val appId = extension.appId ?: throw IllegalStateException("You must define the app id in order to download the manifest")
                val app = project.extensions.getByType(BaseExtension::class.java) as AppExtension
                val appVersion = app.defaultConfig.versionName ?: throw IllegalStateException("No app version number detected")
                it.url.set("https://api.snabble.io/metadata/app/$appId/android/$appVersion")
                it.outputFile = File(baseDir, "src/assets/metadata.json")
            }

            val manifestTask = project.tasks.register(
                "generateSnabbleManifest",
                GenerateMetaTagsTask::class.java
            ) {
                it.appId.set(extension.appId ?: throw IllegalStateException("You must define the app id"))
                it.secret.set(extension.secret ?: throw IllegalStateException("You must define the secret"))
                it.endpoint.set(extension.endpoint)
                it.manifestFile = File(baseDir, "src/AndroidManifest.xml")
            }

            project.extensions.getByType(BaseExtension::class.java).forEachVariant { variant ->
                project.tasks.getByName("generate${variant.capitalizedName()}Resources").dependsOn += generateAarTask
                generateAarTask.dependsOn(manifestTask)
                if (variant.buildType.isDebuggable && extension.prefetchMetaData) {
                    generateAarTask.dependsOn(downloadTask)
                }
            }
            //project.dependencies.add("implementation", project.files(generateAarTask.get().aarFile))
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