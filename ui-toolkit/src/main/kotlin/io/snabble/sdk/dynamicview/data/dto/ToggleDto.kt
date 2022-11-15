package io.snabble.sdk.dynamicview.data.dto

import io.snabble.sdk.dynamicview.domain.model.ToggleItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("toggle")
internal data class ToggleDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("key") val key: String,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun ToggleDto.toToggle(text: String): ToggleItem = ToggleItem(
    id = id,
    text = text,
    key = key,
    padding = padding.toPadding()
)
