package io.snabble.sdk.assetservice.data

import io.snabble.sdk.assetservice.data.dto.AssetDto
import io.snabble.sdk.assetservice.data.dto.ManifestDto
import io.snabble.sdk.assetservice.data.local.LocalAssetDataSource
import io.snabble.sdk.assetservice.data.remote.RemoteAssetsSource
import io.snabble.sdk.assetservice.domain.AssetsRepository
import io.snabble.sdk.assetservice.domain.model.Type
import io.snabble.sdk.assetservice.domain.model.UiMode
import io.snabble.sdk.utils.Logger
import org.apache.commons.io.FilenameUtils

class AssetsRepositoryImpl(
    private val remoteAssetsSource: RemoteAssetsSource,
    private val localAssetDataSource: LocalAssetDataSource
) : AssetsRepository {

    override suspend fun updateAllAssets() {
        Logger.e("Start updating all assets...")
        val manifest: ManifestDto = remoteAssetsSource.downloadManifestForProject() ?: return
        val newAssets = localAssetDataSource.removeExistingAssets(manifest.files)
        Logger.e("Filtered new assets $newAssets")
        Logger.e("Continue with loading all new assets...")
        val assets: List<AssetDto> = remoteAssetsSource.downloadAllAssets(newAssets)
        Logger.e("Saving new assets $assets locally...")
        localAssetDataSource.saveMultipleAssets(assets = assets)
    }

    override suspend fun loadAsset(name: String, type: Type, uiMode: UiMode): AssetDto? =
        getLocalAsset(filename = name.createFileName(type, uiMode))

    private suspend fun getLocalAsset(filename: String): AssetDto? = localAssetDataSource.loadAsset(filename)

    private fun String.createFileName(type: Type, uiMode: UiMode): String {
        val cleanedName = FilenameUtils.removeExtension(this)
        return "$cleanedName${uiMode.value}${type.value}"
    }
}
