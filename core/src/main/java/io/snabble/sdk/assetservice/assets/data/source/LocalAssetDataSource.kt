@file:Suppress("TooGenericExceptionCaught")

package io.snabble.sdk.assetservice.assets.data.source

import com.google.gson.annotations.SerializedName
import io.snabble.sdk.Project
import io.snabble.sdk.assetservice.assets.data.source.dto.AssetDto
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

interface LocalAssetDataSource {

    suspend fun loadAsset(name: String): AssetDto?
    suspend fun saveMultipleAssets(assets: List<AssetDto>)
    fun assetExists(name: String): Boolean
    fun listAssets(): List<String>
    suspend fun deleteAsset(names: List<String>)
    suspend fun cleanupOrphanedFiles()
}

class LocalAssetDataSourceImpl(
    private val project: Project,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocalAssetDataSource {

    private val assetsDir = File(project.internalStorageDirectory, "assets/")
    private val manifestFile = File(project.internalStorageDirectory, "assets_v2.json")

    private var manifest = ManifestFile()

    init {
        assetsDir.mkdirs()
        loadManifest()
    }

    override suspend fun loadAsset(name: String): AssetDto? = withContext(dispatcher) {
        try {
            Logger.d("Loading asset $name...")

            return@withContext manifest.assets[name]
                ?.takeIf { File(it.filePath).exists() }
                ?.let { file ->

                    Logger.d("Asset loaded for $name")

                    AssetDto(
                        name = name,
                        data = File(file.filePath).inputStream(),
                        hash = file.hash
                    )
                }.also {
                    if (it == null) {
                        Logger.e("Asset $name not found in manifest or file does not exist")
                    }
                }
        } catch (e: Exception) {
            Logger.e("Loading asset failed: ${e.message}")
            return@withContext null
        }
    }

    override suspend fun saveMultipleAssets(assets: List<AssetDto>) = withContext(dispatcher) {
        try {
            assets.forEach { assetDto ->
                Logger.e("Saving asset ${assetDto.name}...")
                val fileName = "${assetDto.hash}_${assetDto.name}"
                val file = File(assetsDir, fileName)
                file.createNewFile()

                Logger.d("Created file for ${assetDto.name}")


                assetDto.data.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                Logger.d("Saved asset content into a file ${file.absolutePath}")

                val assetFile = AssetFile(
                    filePath = file.absolutePath,
                    hash = assetDto.hash
                )

                withContext(Dispatchers.Main) {
                    manifest.assets[assetDto.name] = assetFile
                }
            }

            if (assets.isNotEmpty()) saveManifest()
        } catch (e: Exception) {
            Logger.e("Saving Assets failed: ${e.message}")
        }
    }

    private suspend fun saveManifest() = withContext(dispatcher) {
        try {
            val jsonString = GsonHolder.get().toJson(manifest)
            manifestFile.writeText(jsonString)
            Logger.d("Saved manifest for project ${project.id}")
        } catch (e: Exception) {
            Logger.d("Could not save manifest: ${e.message}")
            throw e
        }
    }

    private fun loadManifest() {
        try {
            if (manifestFile.exists()) {
                val jsonString = manifestFile.readText()
                manifest = GsonHolder.get().fromJson(jsonString, ManifestFile::class.java)
                Logger.d("Loaded manifest for project ${project.id} with ${manifest.assets.size} assets")
            }
            Logger.e("Manifest does not exist, creating new one")
            manifest = ManifestFile()
        } catch (e: Exception) {
            Logger.e("Could not load manifest, creating new one: ${e.message}")
            manifest = ManifestFile()
        }
    }

    override fun listAssets(): List<String> = manifest.assets.keys.toList()

    override fun assetExists(name: String): Boolean = manifest.assets.contains(name)

    override suspend fun deleteAsset(names: List<String>) = withContext(dispatcher) {
        try {
            Logger.e("Start deleting dead assets...")
            names.forEach { name ->
                val asset = manifest.assets[name] ?: return@forEach

                val file = File(asset.filePath)
                val deleted = if (file.exists()) file.delete() else true

                if (deleted) {
                    manifest.assets.remove(name)
                    saveManifest()
                }
            }
            Logger.e("Deleted dead assets")
        } catch (e: Exception) {
            Logger.e("Deletion of dead assets failed: ${e.message}")
        }
    }

    override suspend fun cleanupOrphanedFiles() = withContext(dispatcher) {
        try {
            val files = assetsDir.listFiles() ?: return@withContext
            val manifestFilePaths = manifest.assets.values.map { it.filePath }.toSet()

            files.filterNotNull()
                .filter { file -> file.isFile && !manifestFilePaths.contains(file.absolutePath) }
                .map { file ->
                    Logger.d("Deleting orphaned file: ${file.name}")
                    file.delete()
                }
        } catch (e: Exception) {
            Logger.e("Failed to delete orphaned files: ${e.message}")
        }
    }
}

private data class AssetFile(
    @SerializedName("filePath") val filePath: String,
    @SerializedName("hash") val hash: String
)

private data class ManifestFile(
    @SerializedName("assets") val assets: MutableMap<String, AssetFile> = mutableMapOf()
)
