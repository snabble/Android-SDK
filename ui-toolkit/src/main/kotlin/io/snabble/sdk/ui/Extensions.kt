package io.snabble.sdk.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import io.snabble.sdk.data.PaddingDto
import io.snabble.sdk.domain.CustomerCardItem
import io.snabble.sdk.domain.InformationItem
import io.snabble.sdk.domain.Padding

internal fun Padding.toPaddingValues(): PaddingValues =
    PaddingValues(start = start.dp, top = top.dp, end = end.dp, bottom = bottom.dp)

internal fun PaddingDto.toPadding() = Padding(start, top, end, bottom)

internal fun CustomerCardItem.toInformationItem(): InformationItem = InformationItem(
    id = id,
    text = text,
    imageSource = imageSource,
    padding = padding
)
