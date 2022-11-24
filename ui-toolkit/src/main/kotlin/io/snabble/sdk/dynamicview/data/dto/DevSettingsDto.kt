package io.snabble.sdk.dynamicview.data.dto

import io.snabble.sdk.dynamicview.domain.model.DevSettingsItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("snabble.devSettings")
internal data class DevSettingsDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun DevSettingsDto.toDevSettingsItem(
    text: String,
): DevSettingsItem = DevSettingsItem(
    id = id,
    text = text,
    padding = padding.toPadding()
)
