package io.snabble.sdk.screens.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.dynamicview.domain.model.Configuration
import io.snabble.sdk.dynamicview.domain.model.DynamicConfig
import io.snabble.sdk.dynamicview.domain.model.ImageItem
import io.snabble.sdk.dynamicview.domain.model.LocationPermissionItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.ProjectId
import io.snabble.sdk.dynamicview.domain.model.PurchasesItem
import io.snabble.sdk.dynamicview.domain.model.SectionItem
import io.snabble.sdk.dynamicview.domain.model.SeeAllStoresItem
import io.snabble.sdk.dynamicview.domain.model.StartShoppingItem
import io.snabble.sdk.dynamicview.domain.model.TextItem
import io.snabble.sdk.dynamicview.domain.model.ToggleItem
import io.snabble.sdk.dynamicview.ui.DynamicView
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.utils.toPaddingValues
import io.snabble.sdk.dynamicview.viewmodel.DynamicViewModel
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.getComposeColor
import io.snabble.sdk.widgets.ImageWidget

@Composable
internal fun DynamicScreen(
    dynamicViewModel: DynamicViewModel,
    onAction: OnDynamicAction = dynamicViewModel::sendAction,
) {
    @OptIn(ExperimentalLifecycleComposeApi::class)
    val configState = dynamicViewModel.dynamicConfig.collectAsStateWithLifecycle()
    val config = configState.value ?: return
    DynamicView(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(LocalContext.current.getComposeColor("snabble_background") ?: Magenta.toArgb())
            ),
        contentPadding = config.configuration.padding.toPaddingValues(),
        background = {
            if (config.configuration.image != null) {
                ImageWidget(
                    model = ImageItem(
                        "background.image",
                        config.configuration.image,
                        Padding(all = 0)
                    ),
                    contentScale = ContentScale.Fit,
                    onAction = onAction,
                )
            }
        },
        widgets = config.widgets,
        onAction = onAction,
    )
}

@Preview(
    backgroundColor = 0xFFFFFF,
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun HomeScreenPreview() {
    val viewModel: DynamicViewModel = DynamicViewModel()
        .apply {
            val config = DynamicConfig(
                configuration = Configuration(
                    image = R.drawable.home_default_background,
                    style = "",
                    padding = Padding(16)
                ),
                widgets = listOf(
                    TextItem(
                        id = "hello.world.text",
                        text = "Willkommen bei Snabble",
                        textColorSource = MaterialTheme.colorScheme.primary.toArgb(),
                        textStyleSource = "header",
                        showDisclosure = false,
                        padding = Padding(start = 16, top = 16, end = 16, bottom = 0),
                    ),
                    TextItem(
                        id = "title",
                        text = "Deine App für Scan and Go!",
                        textColorSource = MaterialTheme.colorScheme.primary.toArgb(),
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
                    ),
                    PurchasesItem(
                        id = "last.purchases",
                        projectId = ProjectId("0123"),
                        padding = Padding(0),
                    ),
                )
            )
            setConfig(config)
        }
    DynamicScreen(dynamicViewModel = viewModel)
}

@Preview(
    backgroundColor = 0xFFFFFF,
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun ProfileScreenPreview() {
    val viewModel: DynamicViewModel = DynamicViewModel()
        .apply {
            val config = DynamicConfig(
                configuration = Configuration(
                    image = null,
                    style = "",
                    padding = Padding(0)
                ),
                widgets = listOf(
                    SectionItem(
                        id = "section",
                        header = "Profil",
                        items = listOf(
                            ToggleItem(
                                id = "setup.toggle",
                                text = "Show setup",
                                key = "pref.setup.toggle",
                                padding = Padding(horizontal = 16, vertical = 5),
                            ),
                            TextItem(
                                id = "1",
                                text = "Willkommen bei Snabble",
                                textStyleSource = "title",
                                showDisclosure = false,
                                padding = Padding(horizontal = 16, vertical = 5),
                            ),
                        ),
                        padding = Padding(0, 0, 0, 0)
                    )
                )
            )
            setConfig(config)
        }
    DynamicScreen(dynamicViewModel = viewModel)
}
