package io.snabble.sdk.dynamicview.domain.model

data class StartShoppingItem(
    override val id: String,
    val padding: Padding,
) : Widget
