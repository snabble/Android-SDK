package io.snabble.sdk.dynamicview.domain.model

data class PurchasesItem(
    override val id: String,
    val padding: Padding,
) : Widget
