package io.snabble.sdk.dynamicview.data.dto

import io.snabble.sdk.dynamicview.domain.model.ClientIdItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("snabble.clientId")
internal data class ClientIdDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun ClientIdDto.toClientId(): ClientIdItem = ClientIdItem(
    id = id,
    padding = padding.toPadding()
)
