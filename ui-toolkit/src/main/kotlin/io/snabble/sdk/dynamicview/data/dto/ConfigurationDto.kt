package io.snabble.sdk.dynamicview.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConfigurationDto(
    @SerialName("image") val image: String,
    @SerialName("style") val style: String,
    @SerialName("padding") val padding: PaddingDto,
)
