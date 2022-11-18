package io.snabble.sdk.dynamicview.data.dto

import androidx.annotation.ColorInt
import io.snabble.sdk.dynamicview.domain.model.TextItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("text")
internal data class TextDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("textColor") val textColor: String? = null,
    @SerialName("textStyle") val textStyle: String? = null,
    @SerialName("showDisclosure") val showDisclosure: Boolean? = null,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun TextDto.toText(
    text: String,
    @ColorInt textColor: Int?,
): TextItem = TextItem(
    id = id,
    text = text,
    textColor = textColor,
    textStyle = textStyle,
    showDisclosure = showDisclosure ?: false,
    padding = padding.toPadding()
)
