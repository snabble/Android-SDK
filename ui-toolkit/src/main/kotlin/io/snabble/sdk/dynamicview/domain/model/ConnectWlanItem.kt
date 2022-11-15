package io.snabble.sdk.dynamicview.domain.model

data class ConnectWlanItem(
    override val id: String,
    val padding: Padding,
) : Widget
