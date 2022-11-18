package io.snabble.sdk.dynamicview.data.dto

import androidx.annotation.DrawableRes
import io.snabble.sdk.dynamicview.domain.model.ImageItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("image")
internal data class ImageDto(
    @SerialName("id") override val id: String,
    @SerialName("image") val image: String,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun ImageDto.toImage(@DrawableRes image: Int?): ImageItem = ImageItem(
    id = id,
    image = image,
    padding = padding.toPadding()
)
