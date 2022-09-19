package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.ui.R

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun ImageWidgetPreview() {
    ImageWidget(
        model = ImageItem(
            id = "an.image",
            imageSource = R.drawable.snabble_ic_small_chevron_down,
            padding = 8
        )
    )
}

@Composable
fun ImageWidget(
    modifier: Modifier = Modifier,
    model: ImageItem,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
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
