package io.snabble.sdk.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import io.snabble.sdk.data.PaddingDto
import io.snabble.sdk.domain.CustomerCardItem
import io.snabble.sdk.domain.InformationItem
import io.snabble.sdk.domain.Padding

fun Padding.toPaddingValues(): PaddingValues =
    PaddingValues(start = start.dp, top = top.dp, end = end.dp, bottom = bottom.dp)

operator fun Padding.plus(other: Padding): Padding =
    Padding(
        start + other.start,
        top + other.top,
        end + other.end,
        bottom + other.bottom
    )

fun PaddingDto.toPadding() = Padding(start, top, end, bottom)

fun CustomerCardItem.toInformationItem(): InformationItem = InformationItem(
    id = id,
    text = text,
    imageSource = imageSource,
    padding = padding
)
