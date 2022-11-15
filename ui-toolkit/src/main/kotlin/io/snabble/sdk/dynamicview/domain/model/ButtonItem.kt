package io.snabble.sdk.dynamicview.domain.model

import androidx.annotation.ColorRes

data class ButtonItem(
    override val id: String,
    val text: String,
    @ColorRes val foregroundColor: Int?,
    @ColorRes val backgroundColor: Int?,
    val padding: Padding,
) : Widget
