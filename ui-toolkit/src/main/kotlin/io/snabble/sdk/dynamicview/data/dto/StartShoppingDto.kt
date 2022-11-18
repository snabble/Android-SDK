package io.snabble.sdk.dynamicview.data.dto

import io.snabble.sdk.dynamicview.domain.model.StartShoppingItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("snabble.startShopping")
internal data class StartShoppingDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun StartShoppingDto.toStartShopping(): StartShoppingItem = StartShoppingItem(
    id = id,
    padding = padding.toPadding()
)
