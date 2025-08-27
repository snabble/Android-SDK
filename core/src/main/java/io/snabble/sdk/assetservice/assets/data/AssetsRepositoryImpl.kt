package io.snabble.sdk.assetservice.assets.data

import io.snabble.sdk.assetservice.assets.data.source.LocalAssetDataSource
import io.snabble.sdk.assetservice.assets.data.source.RemoteAssetsSource
import io.snabble.sdk.assetservice.assets.data.source.dto.AssetDto
import io.snabble.sdk.assetservice.assets.data.source.dto.ManifestDto
import io.snabble.sdk.assetservice.assets.domain.AssetsRepository
import io.snabble.sdk.assetservice.assets.domain.model.Asset
import io.snabble.sdk.assetservice.domain.model.Type
import io.snabble.sdk.assetservice.domain.model.UiMode
import io.snabble.sdk.utils.Logger
import org.apache.commons.io.FilenameUtils

class AssetsRepositoryImpl(
    private val remoteAssetsSource: RemoteAssetsSource,
    private val localAssetDataSource: LocalAssetDataSource
) : AssetsRepository {

    override suspend fun updateAllAssets() {
        Logger.d("Start updating all assets. Loading manifest...")
        val manifest: ManifestDto = loadManifest() ?: return

        removeDeletedAssets(manifest)

        Logger.d("Clean up orphaned files...")
        localAssetDataSource.cleanupOrphanedFiles()

        val newAssets = manifest.files.filterNot { localAssetDataSource.assetExists(it.name) }
        Logger.d("Filtered new assets $newAssets")

        Logger.d("Continue with loading all new assets...")
        val assets: List<AssetDto> = remoteAssetsSource.downloadAllAssets(newAssets)

        Logger.d("Saving new assets $assets locally...")
        localAssetDataSource.saveMultipleAssets(assets = assets)
    }

    private suspend fun removeDeletedAssets(manifest: ManifestDto) {
        val remoteAssetNames: Set<String> = manifest.files.map { it.name }.toSet()
        val deadAssets: List<String> = localAssetDataSource.listAssets().filterNot { it in remoteAssetNames }
        Logger.d("Removing deleted assets $deadAssets...")
        localAssetDataSource.deleteAsset(deadAssets)
    }

    private suspend fun loadManifest(): ManifestDto? = remoteAssetsSource.downloadManifest().also {
        if (it == null) Logger.e("Manifest couldn't be loaded")
    }

    override suspend fun loadAsset(name: String, type: Type, uiMode: UiMode): Asset? =
        getLocalAsset(filename = name.createFileName(type, uiMode))?.toModel()

    private suspend fun getLocalAsset(filename: String): AssetDto? = localAssetDataSource.loadAsset(filename)

    private fun String.createFileName(type: Type, uiMode: UiMode): String {
        val cleanedName = FilenameUtils.removeExtension(this)
        return "$cleanedName${uiMode.value}${type.value}"
    }
}

private fun AssetDto.toModel() = Asset(name = name, hash = hash, data = data)
