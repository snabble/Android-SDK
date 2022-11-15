package io.snabble.sdk.dynamicview.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("section")
internal data class SectionDto(
    @SerialName("id") override val id: String,
    @SerialName("header") val header: String,
    @SerialName("items") val widgets: List<WidgetDto>,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto
