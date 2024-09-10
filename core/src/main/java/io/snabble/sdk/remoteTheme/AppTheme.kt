package io.snabble.sdk.remoteTheme

import com.google.gson.annotations.SerializedName

data class AppTheme(
    val lightModeColors: LightModeColors? = null,
    val darkModeColors: DarkModeColors? = null,
)

data class LightModeColors(
    @SerializedName("colorPrimary_light") val primaryColor: String,
    @SerializedName("colorOnPrimary_light") val onPrimaryColor: String,
    @SerializedName("colorSecondary_light") val secondaryColor: String,
    @SerializedName("colorOnSecondary_light") val onSecondaryColor: String
)

data class DarkModeColors(
    @SerializedName("colorPrimary_dark") val primaryColor: String,
    @SerializedName("colorOnPrimary_dark") val onPrimaryColor: String,
    @SerializedName("colorSecondary_dark") val secondaryColor: String,
    @SerializedName("colorOnSecondary_dark") val onSecondaryColor: String
)
