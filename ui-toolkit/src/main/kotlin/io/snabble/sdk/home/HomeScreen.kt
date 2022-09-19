package io.snabble.sdk.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.ButtonItem
import io.snabble.sdk.domain.Configuration
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.Root
import io.snabble.sdk.domain.SpacerItem
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.ui.DynamicView
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.widgets.ImageWidget
import io.snabble.sdk.utils.getComposeColor

@Preview(
    backgroundColor = 0xFFFFFF,
    showBackground = true,
    showSystemUi = true,
)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        homeConfig = Root(
            configuration = Configuration(
                image = R.drawable.home_default_background,
                style = "",
                padding = 8
            ),
            widgets = listOf(
                SpacerItem(length = 16),
                TextItem(
                    id = "hello.world.text",
                    text = "Willkommen bei Snabble",
                    textColorSource = LocalContext.current.getComposeColor("snabble_onboarding_primary"),
                    textStyleSource = "header",
                    showDisclosure = false,
                    padding = 16
                ),
                TextItem(
                    id = "title",
                    text = "Deine App für Scan and Go!",
                    textColorSource = LocalContext.current.getComposeColor(null),
                    textStyleSource = "body",
                    showDisclosure = false,
                    padding = 16
                ),
                SpacerItem(length = 10),
                TextItem(
                    id = "brand",
                    text = "Snabble",
                    textColorSource = null,
                    textStyleSource = "footer",
                    showDisclosure = false,
                    padding = 16,
                ),
                SpacerItem(length = 5),
                ButtonItem(
                    id = "stores.button",
                    text = "See all stores",
                    foregroundColorSource = null,
                    backgroundColorSource = LocalContext.current
                        .getComposeColor("snabble_onboarding_primary"),
                    padding = 5,
                ),
                SpacerItem(length = 5),
            )
        ),
    )
}

@Composable
fun HomeScreen(
    homeConfig: Root,
) {
    DynamicView(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(
                    LocalContext.current.getComposeColor("snabble_background") ?: Magenta.toArgb()
                )
            ),
        background = {
            if (homeConfig.configuration.image != null) {
                ImageWidget(
                    model = ImageItem("background.image", homeConfig.configuration.image, 8),
                    contentScale = ContentScale.Fit,
                )
            }
        },
        widgets = homeConfig.widgets,
        onClick = { widgetId ->
            Log.i("HomeScreen", "::onClick widgetId -> $widgetId")
        },
    )
}
