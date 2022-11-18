package io.snabble.sdk.dynamicview.data.dto

import io.snabble.sdk.dynamicview.domain.model.SeeAllStoresItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("snabble.allStores")
internal data class SeeAllStoresDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun SeeAllStoresDto.toSeeAllStores(): SeeAllStoresItem = SeeAllStoresItem(
    id = id,
    padding = padding.toPadding()
)
