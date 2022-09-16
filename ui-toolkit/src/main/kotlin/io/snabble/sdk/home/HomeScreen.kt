package io.snabble.sdk.home

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.Configuration
import io.snabble.sdk.domain.Image
import io.snabble.sdk.domain.Root
import io.snabble.sdk.ui.DynamicView
import io.snabble.sdk.ui.ImageWidget
import io.snabble.sdk.ui.WidgetFactory
import io.snabble.sdk.ui.toolkit.R

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
            widgets = listOf()
        ),
        object : WidgetFactory {}
    )
}

@Composable
fun HomeScreen(
    homeConfig: Root,
    widgetFactory: WidgetFactory,
) {
    DynamicView(
        widgetFactory = widgetFactory,
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
        }
    )
}