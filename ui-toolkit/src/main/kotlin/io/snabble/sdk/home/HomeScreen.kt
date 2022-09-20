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
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.ProjectId
import io.snabble.sdk.domain.PurchasesItem
import io.snabble.sdk.domain.Root
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
                padding = Padding(horizontal = 8),
            ),
            widgets = listOf(
                TextItem(
                    id = "hello.world.text",
                    text = "Willkommen bei Snabble",
                    textColorSource = LocalContext.current.getComposeColor("snabble_onboarding_primary"),
                    textStyleSource = "header",
                    showDisclosure = false,
                    padding = Padding(start = 16, top = 16, end = 16, bottom = 0),
                ),
                TextItem(
                    id = "title",
                    text = "Deine App für Scan and Go!",
                    textColorSource = LocalContext.current.getComposeColor(null),
                    textStyleSource = "body",
                    showDisclosure = false,
                    padding = Padding(horizontal = 16),
                ),
                TextItem(
                    id = "brand",
                    text = "Snabble",
                    textColorSource = null,
                    textStyleSource = "footer",
                    showDisclosure = false,
                    padding = Padding(start = 16, top = 10, end = 16, bottom = 0),
                ),
                ButtonItem(
                    id = "stores.button",
                    text = "See all stores",
                    foregroundColorSource = null,
                    backgroundColorSource = LocalContext.current
                        .getComposeColor("snabble_onboarding_primary"),
                    padding = Padding(start = 16, top = 5, end = 16, bottom = 5),
                ),
                PurchasesItem(
                    id = "last.purchases",
                    projectId = ProjectId("0123"),
                    padding = Padding(horizontal = 16),
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
                    model = ImageItem(
                        "background.image",
                        homeConfig.configuration.image,
                        Padding(all = 0)
                    ),
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
