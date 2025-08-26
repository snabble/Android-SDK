package io.snabble.sdk.assetservice.data.dto

import java.io.InputStream

data class AssetDto(
    val name: String,
    val hash: String,
    val data: InputStream,
)
