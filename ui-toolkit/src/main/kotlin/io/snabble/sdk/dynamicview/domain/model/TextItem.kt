package io.snabble.sdk.dynamicview.domain.model

import androidx.annotation.ColorInt

data class TextItem(
    override val id: String,
    val text: String,
    @param:ColorInt val textColor: Int? = null,
    val textStyle: String? = null,
    val showDisclosure: Boolean,
    val padding: Padding,
) : Widget
