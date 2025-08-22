package io.snabble.sdk.assetservice.data.dto

import com.google.gson.annotations.SerializedName

data class AssetVariantDto(
    @SerializedName("name") var name: String,
    @SerializedName("variants") var variants: MutableMap<VariantDto, String> = mutableMapOf()
)
