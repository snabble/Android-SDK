package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.InformationItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.toPaddingValues

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun InformationWidgetPreview() {
    ImageWidget(
        model = ImageItem(
            id = "an.image",
            imageSource = R.drawable.snabble_ic_small_chevron_down,
            padding = Padding(start = 8, top = 0, end = 8, bottom = 0),
        )
    )
}

@Composable
fun InformationWidget(
    modifier: Modifier = Modifier,
    model: InformationItem,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(model.padding.toPaddingValues()),
    ) {
        if (model.imageSource != null) {
            Image(
                modifier = modifier.fillMaxWidth(),
                contentScale = contentScale,
                painter = painterResource(id = model.imageSource),
                contentDescription = "",
            )
        }
    }
}