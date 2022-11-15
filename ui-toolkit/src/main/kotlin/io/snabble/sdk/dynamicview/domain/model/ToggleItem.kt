package io.snabble.sdk.dynamicview.domain.model

data class ToggleItem(
    override val id: String,
    val text: String,
    val key: String,
    val padding: Padding,
) : Widget
