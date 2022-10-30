package io.snabble.sdk.dynamicview.domain.model.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import io.snabble.sdk.dynamicview.data.PaddingDto
import io.snabble.sdk.dynamicview.domain.model.CustomerCardItem
import io.snabble.sdk.dynamicview.domain.model.InformationItem
import io.snabble.sdk.dynamicview.domain.model.Padding

internal fun Padding.toPaddingValues(): PaddingValues =
    PaddingValues(start = start.dp, top = top.dp, end = end.dp, bottom = bottom.dp)

internal fun PaddingDto.toPadding() = Padding(start, top, end, bottom)

internal fun CustomerCardItem.toInformationItem(): InformationItem = InformationItem(
    id = id,
    text = text,
    image = image,
    padding = padding
)
