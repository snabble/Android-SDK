package io.snabble.sdk.home

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.Configuration
import io.snabble.sdk.domain.Image
import io.snabble.sdk.domain.Root
import io.snabble.sdk.domain.Text
import io.snabble.sdk.ui.DynamicView
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.ui.widgets.ImageWidget

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        homeConfig = Root(
            configuration = Configuration(
                image = R.drawable.snabble_gps_fixed,
                style = "",
                padding = 8
            ),
            widgets = listOf(
                Text(
                    id = "hello.world.text",
                    text = "Willkommen bei Snabble",
                    textColorSource = LocalContext.current.getComposeColor("snabble_onboarding_primary"),
                    textStyleSource = "header",
                    showDisclosure = false,
                    spacing = 0,
                    padding = 16
                ),
                Text(
                    id = "title",
                    text = "Deine App für Scan and Go!",
                    textColorSource = LocalContext.current.getComposeColor(null),
                    textStyleSource = "body",
                    showDisclosure = false,
                    spacing = 10,
                    padding = 16
                ),
                Text(
                    id = "brand",
                    text = "Snabble",
                    textColorSource = null,
                    textStyleSource = "footer",
                    showDisclosure = false,
                    spacing = 5,
                    padding = 16
                ),
            )
        ),
    )
}

@Composable
fun HomeScreen(
    homeConfig: Root,
) {
    DynamicView(
        background = {
            if (homeConfig.configuration.image != null) {
                ImageWidget(
                    model = Image("background.image", homeConfig.configuration.image, 0, 8),
                    contentScale = ContentScale.Crop,
                )
            }
        },
        widgets = homeConfig.widgets,
        onClick = { widgetId ->
            Log.i("HomeScreen", "::onClick widgetId -> $widgetId")
        },
    )
}
