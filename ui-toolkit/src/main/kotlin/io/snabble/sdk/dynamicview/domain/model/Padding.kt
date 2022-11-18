package io.snabble.sdk.dynamicview.domain.model

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

data class Padding(
    val start: Int = 0,
    val top: Int = 0,
    val end: Int = 0,
    val bottom: Int = 0,
) {

    constructor(all: Int)
            : this(start = all, top = all, end = all, bottom = all)

    constructor(horizontal: Int = 0, vertical: Int = 0)
            : this(start = horizontal, top = vertical, end = horizontal, bottom = vertical)
}

internal fun Padding.toPaddingValues(): PaddingValues =
    PaddingValues(start = start.dp, top = top.dp, end = end.dp, bottom = bottom.dp)
