package io.snabble.sdk.dynamicview.data.dto

import io.snabble.sdk.dynamicview.domain.model.PurchasesItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("purchases")
internal data class PurchasesDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun PurchasesDto.toPurchases(): PurchasesItem = PurchasesItem(
    id = id,
    padding = padding.toPadding()
)
