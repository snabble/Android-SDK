package io.snabble.sdk.home

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
import androidx.lifecycle.viewmodel.compose.viewModel
import io.snabble.sdk.domain.Configuration
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.LocationPermissionItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.Root
import io.snabble.sdk.domain.SeeAllStoresItem
import io.snabble.sdk.domain.StartShoppingItem
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.ui.AppTheme
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
                padding = Padding(horizontal = 8, vertical = 0),
            ),
            widgets = listOf(
                TextItem(
                    id = "hello.world.text",
                    text = "Willkommen bei Snabble",
                    textColorSource = AppTheme.colors.snabble_primaryColor.toArgb(),
                    textStyleSource = "header",
                    showDisclosure = false,
                    padding = Padding(start = 16, top = 16, end = 16, bottom = 0),
                ),
                TextItem(
                    id = "title",
                    text = "Deine App für Scan and Go!",
                    textColorSource = AppTheme.colors.snabble_textColorDark.toArgb(),
                    textStyleSource = "body",
                    showDisclosure = false,
                    padding = Padding(16, 0),
                ),
                TextItem(
                    id = "brand",
                    text = "Snabble",
                    textColorSource = null,
                    textStyleSource = "footer",
                    showDisclosure = false,
                    padding = Padding(start = 16, top = 10, end = 16, bottom = 0),
                ),
                StartShoppingItem(
                    id = "start",
                    padding = Padding(start = 16, top = 5, end = 16, bottom = 5),
                ),
                SeeAllStoresItem(
                    id = "stores",
                    padding = Padding(start = 16, top = 5, end = 16, bottom = 5),
                ),
                LocationPermissionItem(
                    id = "location",
                    padding = Padding(start = 16, top = 5, end = 16, bottom = 5),
                )
            )
        ),
    )
}

@Composable
fun HomeScreen(
    homeConfig: Root,
    viewModel: HomeViewModel = viewModel()
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
            viewModel.onClick(widgetId)
        },
    )
}
