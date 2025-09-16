package io.snabble.sdk.assetservice.assets.data.source.dto

import com.google.gson.annotations.SerializedName

internal data class AssetVariantDto(
    @SerializedName("name") var name: String,
    @SerializedName("variants") var variants: MutableMap<VariantDto, String> = mutableMapOf()
)
