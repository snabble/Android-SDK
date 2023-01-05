package io.snabble.sdk.dynamicview.data.dto

import io.snabble.sdk.dynamicview.domain.model.SeeStoresItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("snabble.allStores")
internal data class AllStoresDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun AllStoresDto.toAllStores(): SeeStoresItem = SeeStoresItem(
    id = id,
    padding = padding.toPadding()
)
