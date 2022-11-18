package io.snabble.sdk.dynamicview.data.dto

import androidx.annotation.DrawableRes
import io.snabble.sdk.dynamicview.domain.model.CustomerCardItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("snabble.customerCard")
internal data class CustomerCardDto(
    @SerialName("id") override val id: String,
    @SerialName("text") val text: String,
    @SerialName("image") val image: String? = null,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun CustomerCardDto.toCustomCardItem(
    text: String,
    @DrawableRes image: Int?,
): CustomerCardItem = CustomerCardItem(
    id = id,
    text = text,
    image = image,
    padding = padding.toPadding()
)
