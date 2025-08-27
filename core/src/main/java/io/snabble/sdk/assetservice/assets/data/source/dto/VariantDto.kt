package io.snabble.sdk.assetservice.assets.data.source.dto

import com.google.gson.annotations.SerializedName

enum class VariantDto(var factor: String?, var density: Float) {
    @SerializedName("1x")
    MDPI(factor = "1x", density = 1.0f),

    @SerializedName("1.5x")
    HDPI(factor = "1.5x", density = 1.5f),

    @SerializedName("2x")
    XHDPI(factor = "2x", density = 2.0f),

    @SerializedName("3x")
    XXHDPI(factor = "3x", density = 3.0f),

    @SerializedName("4x")
    XXXHDPI(factor = "4x", density = 4.0f);
}
