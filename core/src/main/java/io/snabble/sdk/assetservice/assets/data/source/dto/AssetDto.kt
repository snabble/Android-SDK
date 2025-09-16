package io.snabble.sdk.assetservice.assets.data.source.dto

import java.io.InputStream

internal data class AssetDto(
    val name: String,
    val hash: String,
    val data: InputStream,
)
