package io.snabble.sdk.dynamicview.domain.model

data class SeeStoresItem(
    override val id: String,
    val padding: Padding,
) : Widget
