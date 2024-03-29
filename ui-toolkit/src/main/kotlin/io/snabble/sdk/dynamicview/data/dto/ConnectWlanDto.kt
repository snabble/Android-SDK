package io.snabble.sdk.dynamicview.data.dto

import io.snabble.sdk.dynamicview.domain.model.ConnectWlanItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("snabble.connectWifi")
internal data class ConnectWlanDto(
    @SerialName("id") override val id: String,
    @SerialName("padding") val padding: PaddingDto,
) : WidgetDto

internal fun interface SsidProvider {

    fun provide(): String?
}

internal fun ConnectWlanDto.toConnectWlan(ssidProvider: SsidProvider): ConnectWlanItem = ConnectWlanItem(
    id = id,
    padding = padding.toPadding(),
    ssidProvider.provide(),
)
