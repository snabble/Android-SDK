package io.snabble.sdk.assetservice.data.dto

import com.google.gson.annotations.SerializedName

data class ManifestDto(
    @SerializedName("files") val files: List<AssetVariantDto>
)
