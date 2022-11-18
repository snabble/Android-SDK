package io.snabble.sdk.dynamicview.domain.model

data class InformationItem(
    override val id: String,
    val text: String,
    val image: Int?,
    val padding: Padding,
) : Widget

internal fun CustomerCardItem.toInformationItem(): InformationItem = InformationItem(
    id = id,
    text = text,
    image = image,
    padding = padding
)
