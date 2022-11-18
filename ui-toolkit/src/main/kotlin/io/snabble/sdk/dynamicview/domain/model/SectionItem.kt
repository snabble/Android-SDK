package io.snabble.sdk.dynamicview.domain.model

data class SectionItem(
    override val id: String,
    val header: String,
    val items: List<Widget>,
    val padding: Padding,
) : Widget
