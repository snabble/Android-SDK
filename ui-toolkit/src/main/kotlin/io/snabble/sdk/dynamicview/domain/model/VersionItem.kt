package io.snabble.sdk.dynamicview.domain.model

data class VersionItem(
    override val id: String,
    val padding: Padding,
) : Widget
