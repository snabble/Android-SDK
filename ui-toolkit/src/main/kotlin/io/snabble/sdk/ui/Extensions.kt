package io.snabble.sdk.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import io.snabble.sdk.domain.Padding

fun Padding.toPaddingValues(): PaddingValues =
    PaddingValues(start = start.dp, top = top.dp, end = end.dp, bottom = bottom.dp)
