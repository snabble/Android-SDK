package io.snabble.sdk.dynamicview.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DynamicConfigDto(
    @SerialName("configuration") val configuration: ConfigurationDto,
    @SerialName("widgets") val widgets: List<WidgetDto>,
)
