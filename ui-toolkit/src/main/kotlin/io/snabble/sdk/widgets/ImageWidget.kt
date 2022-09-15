package io.snabble.sdk.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.snabble.sdk.utils.getImageId

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, showSystemUi = true)
@Composable
fun ImageWidgetPreview() {
    ImageWidget(
        model = ImageModel(
            id = 1,
            imageSource = "R.drawable.snabble_onboarding_step1",
            spacing = 8,
        )
    )
}

@Composable
fun ImageWidget(
    model: ImageModel,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (model.imageSource.startsWith("http", ignoreCase = true)) {
            AsyncImage(
                modifier = Modifier.wrapContentSize(),
                contentScale = contentScale,
                model = model.imageSource,
                contentDescription = "",
            )
        } else {
            Image(
                modifier = Modifier.fillMaxWidth(),
                contentScale = contentScale,
                painter = painterResource(id = LocalContext.current.getImageId(model.imageSource)),
                // painter = painterResource(id = R.drawable.snabble_onboarding_step1),
                contentDescription = "",
            )
        }
        Spacer(modifier = Modifier.height(model.spacing.dp))
    }
}
