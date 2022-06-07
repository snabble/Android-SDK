package io.snabble.setup

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.io.IOException

@CacheableTask
abstract class GenerateMetaTagsTask : DefaultTask() {
    @get:Input
    abstract val appId: Property<String>
    @get:Input
    abstract val secret: Property<String>
    @get:Input
    @get:Optional
    abstract val endpoint: Property<String?>

    @get:OutputFile
    abstract var manifestFile: File

    @TaskAction
    fun generateManifest() {
        val manifestPath = manifestFile.parentFile
        if (manifestPath.exists() && manifestPath.deleteRecursively()) {
            logger.warn("Failed to clear directory for snabble setup")
        }
        if (!manifestPath.mkdirs()) {
            throw IOException("Could not create path $manifestPath")
        }
        val metadata = mapOf(
            "app_id" to appId.get(),
            "secret" to secret.get(),
            "endpoint_baseurl" to endpoint.orNull,
        ).filterNot {
            it.value.isNullOrEmpty()
        }.entries.joinToString("\n") { (key, value) ->
            // white space is intended as it is
            """
                    <meta-data
                        android:name="snabble_$key"
                        android:value="$value" />"""
        }.trimStart()
        val manifest = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="io.snabble.setup">

                <uses-permission android:name="android.permission.INTERNET" />

                <application>
                    $metadata
                </application>
            </manifest>
        """.trimIndent()
        manifestFile.writeText(manifest)
    }
}