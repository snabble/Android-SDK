package io.snabble.sdk.assetservice.assets.data.source.dto

import com.google.gson.annotations.SerializedName

internal data class ManifestDto(
    @SerializedName("files") val files: List<AssetVariantDto>
)
