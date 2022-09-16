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
import io.snabble.sdk.ui.ImageWidget
import io.snabble.sdk.ui.WidgetFactory
import io.snabble.sdk.ui.WidgetFactoryImpl
import io.snabble.sdk.ui.toolkit.R
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
                    id = 1,
                    text = "Willkommen bei Snabble",
                    textColorSource = LocalContext.current.getComposeColor("snabble_onboarding_primary"),
                    textStyleSource = "header",
                    showDisclosure = false,
                    spacing = 0,
                    padding = 16
                ),
                Text(
                    id = 1,
                    text = "Deine App für Scan and Go!",
                    textColorSource = LocalContext.current.getComposeColor("snabble_secondary"),
                    textStyleSource = "body",
                    showDisclosure = false,
                    spacing = 10,
                    padding = 16
                ),
                Text(
                    id = 1,
                    text = "Begib dich in eine Filiale um einzukaufen.",
                    textColorSource = null,
                    textStyleSource = "footer",
                    showDisclosure = false,
                    spacing = 5,
                    padding = 16
                ),
            )
        ),
        widgetFactory = WidgetFactoryImpl()
    )
}

@Composable
fun HomeScreen(
    homeConfig: Root,
    widgetFactory: WidgetFactory,
) {
    DynamicView(
        header = {
            if (homeConfig.configuration.image != null) {
                ImageWidget(
                    model = Image(0, homeConfig.configuration.image, 0, 8),
                    contentScale = ContentScale.Crop,
                )
            }
        },
        widgets = homeConfig.widgets,
        onClick = { widgetId ->
            Log.i("HomeScreen", "::onClick widgetId -> $widgetId")
        },
        widgetFactory = widgetFactory,
    )
}
