package io.snabble.sdk.dynamicview.data.dto

import androidx.annotation.ColorRes
import io.snabble.sdk.dynamicview.domain.model.ButtonItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("button")
internal data class ButtonDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("foregroundColor") val foregroundColor: String? = null,
    @SerialName("backgroundColor") val backgroundColor: String? = null,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun ButtonDto.toButton(
    text: String,
    @ColorRes foregroundColor: Int?,
    @ColorRes backgroundColor: Int?,
): ButtonItem = ButtonItem(
    id = id,
    text = text,
    foregroundColor = foregroundColor,
    backgroundColor = backgroundColor,
    padding = padding.toPadding()
)
