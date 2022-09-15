package io.snabble.sdk.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.domain.Image

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, showSystemUi = true)
@Composable
fun ImageWidgetPreview() {
    ImageWidget(
        model = Image(
            id = 1,
            imageSource = R.drawable.snabble_ic_small_chevron_down,
            spacing = 8,
        )
    )
}

@Composable
fun ImageWidget(
    model: Image,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (model.imageSource != null){
            Image(
                modifier = Modifier.fillMaxWidth(),
                contentScale = contentScale,
                painter = painterResource(id = model.imageSource),
                // painter = painterResource(id = R.drawable.snabble_onboarding_step1),
                contentDescription = "",
            )
        }
    Spacer(modifier = Modifier.height(model.spacing.dp))
    }
}
