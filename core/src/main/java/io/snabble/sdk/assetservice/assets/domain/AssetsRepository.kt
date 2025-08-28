package io.snabble.sdk.assetservice.assets.domain

import io.snabble.sdk.assetservice.assets.domain.model.Asset
import io.snabble.sdk.assetservice.image.domain.model.Type
import io.snabble.sdk.assetservice.image.domain.model.UiMode

internal interface AssetsRepository {

    suspend fun updateAllAssets()

    suspend fun loadAsset(name: String, type: Type, uiMode: UiMode): Asset?
}
