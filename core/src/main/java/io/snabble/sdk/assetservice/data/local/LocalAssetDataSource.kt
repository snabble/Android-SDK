package io.snabble.sdk.assetservice.data.local

import io.snabble.sdk.Project
import io.snabble.sdk.assetservice.data.dto.AssetDto
import io.snabble.sdk.assetservice.data.dto.AssetVariantDto
import io.snabble.sdk.extensions.xx
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream

interface LocalAssetDataSource {

    suspend fun loadAsset(name: String): AssetDto?
    suspend fun saveMultipleAssets(assets: List<AssetDto>)
    suspend fun removeExistingAssets(assets: List<AssetVariantDto>): List<AssetVariantDto>
}

class LocalAssetDataSourceImpl(
    private val project: Project,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : LocalAssetDataSource {

    private val assetsDir = File(project.internalStorageDirectory, "assets/")
    private val manifestFile = File(project.internalStorageDirectory, "assets_v2.json")

    private var manifest = Manifest()

    init {
        assetsDir.mkdirs()
        loadManifest()
    }

    override suspend fun loadAsset(name: String): AssetDto? = withContext(dispatcher) {
        try {
            manifest.assets.contains(name).xx("contains $name")
            val asset = manifest.assets[name]
                ?: throw IllegalArgumentException("Asset '$name' not found in manifest")

            val file = File(asset.filePath)
            if (!file.exists()) {
                throw IllegalStateException("Asset file not found: ${asset.filePath}")
            }

            return@withContext AssetDto(
                name = name,
                data = file.inputStream(),
                hash = asset.hash
            )
        } catch (e: Exception) {
            Logger.e(e.message)
            return@withContext null
        }
    }

    override suspend fun saveMultipleAssets(assets: List<AssetDto>) = withContext(dispatcher) {
        try {
            assets.forEach { assetDto ->
                val fileName = "${assetDto.hash}_${assetDto.name}"
                val file = File(assetsDir, fileName)
                file.createNewFile()
                // Ensure assets directory exists
                if (!assetsDir.exists()) {
                    assetsDir.mkdirs()
                }
                assetDto.data.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                // Create asset entry
                val asset = Asset(
                    filePath = file.absolutePath,
                    hash = assetDto.hash
                )

                withContext(Dispatchers.Main) {
                    "save asset ${assetDto.name}".xx()
                    manifest.assets[assetDto.name] = asset
                }
            }

            // Save manifest once after all assets
            saveManifest()
        } catch (e: Exception) {
            e.xx("Fuak me ${e.cause}")
            Logger.e(e.message)
        }
    }

    override suspend fun removeExistingAssets(assets: List<AssetVariantDto>): List<AssetVariantDto> =
        assets.filterNot { manifest.assets.contains(it.name) }

    private suspend fun saveManifest() = withContext(dispatcher) {
        try {
            val jsonString = GsonHolder.get().toJson(manifest)
            manifestFile.writeText(jsonString)
            println("Saved manifest for project ${project.id}")
        } catch (e: Exception) {
            println("Could not write manifest: ${e.message}")
            throw e
        }
    }

    private fun loadManifest() {
        try {
            if (manifestFile.exists()) {
                val jsonString = manifestFile.readText()
                manifest = GsonHolder.get().fromJson(jsonString, Manifest::class.java)
                println("Loaded manifest for project ${project.id} with ${manifest.assets.size} assets")
            }
        } catch (e: Exception) {
            println("Could not load manifest, creating new one: ${e.message}")
            manifest = Manifest()
        }
    }

    fun listAssets(): List<String> = manifest.assets.keys.toList()

    fun assetExists(name: String): Boolean = manifest.assets.containsKey(name)

    suspend fun deleteAsset(name: String): Result<Boolean> = withContext(dispatcher) {
        runCatching {
            val asset = manifest.assets[name] ?: return@runCatching false

            val file = File(asset.filePath)
            val deleted = if (file.exists()) file.delete() else true

            if (deleted) {
                manifest.assets.remove(name)
                saveManifest()
            }

            deleted
        }
    }

    suspend fun cleanupUnusedAssets(referencedHashes: Set<String>): Result<Int> = withContext(dispatcher) {
        runCatching {
            val removals = mutableListOf<String>()
            var hasChanges = false

            // Find assets to remove
            manifest.assets.forEach { (name, asset) ->
                if (!referencedHashes.contains(asset.hash)) {
                    println("Removing unused asset: $name")

                    // Delete file
                    val file = File(asset.filePath)
                    if (file.exists()) {
                        file.delete()
                    }

                    removals.add(name)
                    hasChanges = true
                }
            }

            // Remove from manifest
            removals.forEach { name ->
                manifest.assets.remove(name)
            }

            // Save manifest if changes were made
            if (hasChanges) {
                saveManifest()
                println("Cleaned up ${removals.size} unused assets for project ${project.id}")
            }

            removals.size
        }
    }

    suspend fun cleanupOrphanedFiles(): Result<Int> = withContext(dispatcher) {
        runCatching {
            val manifestFilePaths = manifest.assets.values.map { it.filePath }.toSet()
            val orphanedFiles = mutableListOf<File>()

            // Find files not in manifest
            assetsDir.listFiles()?.forEach { file ->
                if (file.isFile && !manifestFilePaths.contains(file.absolutePath)) {
                    orphanedFiles.add(file)
                }
            }

            // Delete orphaned files
            orphanedFiles.forEach { file ->
                println("Deleting orphaned file: ${file.name}")
                file.delete()
            }

            println("Cleaned up ${orphanedFiles.size} orphaned files")
            orphanedFiles.size
        }
    }
}

private data class Asset(
    val filePath: String,
    val hash: String
)

private data class Manifest(
    val assets: MutableMap<String, Asset> = mutableMapOf()
)
