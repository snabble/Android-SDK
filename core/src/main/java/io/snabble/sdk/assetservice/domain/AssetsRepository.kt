package io.snabble.sdk.assetservice.domain

import io.snabble.sdk.assetservice.data.dto.AssetDto
import io.snabble.sdk.assetservice.domain.model.Type
import io.snabble.sdk.assetservice.domain.model.UiMode

interface AssetsRepository {

    suspend fun updateAllAssets()

    suspend fun loadAsset(name: String, type: Type, uiMode: UiMode): AssetDto?
}
