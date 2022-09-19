package io.snabble.sdk.home

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.Button
import io.snabble.sdk.domain.Configuration
import io.snabble.sdk.domain.Image
import io.snabble.sdk.domain.Root
import io.snabble.sdk.domain.Text
import io.snabble.sdk.ui.DynamicView
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.widgets.ImageWidget
import io.snabble.sdk.utils.getComposeColor

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
                    text = "Hello World",
                    showDisclosure = false,
                    spacing = 5,
                    padding = 5
                ),
                Text(
                    id = "title",
                    text = "Deine App für Scan and Go!",
                    textStyleSource = "body",
                    showDisclosure = false,
                    spacing = 5,
                    padding = 5
                ),
                Text(
                    id = "brand",
                    text = "Snabble",
                    showDisclosure = false,
                    spacing = 5,
                    padding = 5
                ),
                Button(
                    id = "stores.button",
                    text = "See all stores",
                    foregroundColorSource = null,
                    backgroundColorSource = LocalContext.current
                        .getComposeColor("snabble_onboarding_primary"),
                    spacing = 5,
                    padding = 5
                )
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
