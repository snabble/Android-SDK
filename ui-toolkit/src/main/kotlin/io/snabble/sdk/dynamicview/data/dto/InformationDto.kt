package io.snabble.sdk.dynamicview.data.dto

import androidx.annotation.DrawableRes
import io.snabble.sdk.dynamicview.domain.model.InformationItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("information")
internal data class InformationDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("image") val image: String? = null,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun InformationDto.toInformation(
    text: String,
    @DrawableRes image: Int?,
): InformationItem = InformationItem(
    id = id,
    text = text,
    image = image,
    padding = padding.toPadding()
)
