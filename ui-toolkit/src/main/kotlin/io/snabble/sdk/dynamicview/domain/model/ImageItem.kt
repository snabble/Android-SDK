package io.snabble.sdk.dynamicview.domain.model

import androidx.annotation.DrawableRes

data class ImageItem(
    override val id: String,
    @DrawableRes val image: Int?,
    val padding: Padding,
) : Widget
