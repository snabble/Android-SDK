package io.snabble.setup

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@CacheableTask
abstract class GenerateAarTask : DefaultTask() {
    @get:OutputFile
    abstract var aarFile: File

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract var sourceDir: File

    @TaskAction
    fun generateAarFile() {
        if (!sourceDir.exists()) {
            logger.warn("Nothing to pack")
        }
        if (aarFile.exists()) {
            if (!aarFile.delete()) {
                throw IOException("Could not delete old aar file at '$aarFile'")
            }
        }
        val zos = ZipOutputStream(aarFile.outputStream())
        // allow list
        val filesToPack = listOf("AndroidManifest.xml", "assets/metadata.json")
        filesToPack.map {
            it to File(sourceDir, it)
        }.filter {
            it.second.exists()
        }.forEach { (path, file) ->
            zos.putNextEntry(ZipEntry(path))
            zos.write(file.readBytes())
            zos.closeEntry()
        }
        zos.close()
    }
}