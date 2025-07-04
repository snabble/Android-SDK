package io.snabble.sdk.dynamicview.domain.model

import androidx.annotation.DrawableRes

data class CustomerCardItem(
    override val id: String,
    val text: String,
    @param:DrawableRes val image: Int?,
    val padding: Padding,
) : Widget
