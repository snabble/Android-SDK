package io.snabble.sdk.dynamicview.data.dto

import io.snabble.sdk.dynamicview.data.dto.serializer.PaddingValueListSerializer
import io.snabble.sdk.dynamicview.domain.model.Padding
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = PaddingValueListSerializer::class)
@SerialName("padding")
internal data class PaddingDto(
    @SerialName("left") val start: Int,
    @SerialName("top") val top: Int,
    @SerialName("right") val end: Int,
    @SerialName("bottom") val bottom: Int,
)

internal fun PaddingDto.toPadding() = Padding(start, top, end, bottom)
