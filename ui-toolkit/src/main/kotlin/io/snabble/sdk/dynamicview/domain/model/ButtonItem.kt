package io.snabble.sdk.dynamicview.domain.model

import androidx.annotation.ColorRes

data class ButtonItem(
    override val id: String,
    val text: String,
    @param:ColorRes val foregroundColor: Int?,
    @param:ColorRes val backgroundColor: Int?,
    val padding: Padding,
) : Widget
