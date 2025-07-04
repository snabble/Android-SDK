package io.snabble.sdk.dynamicview.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.snabble.sdk.dynamicview.domain.model.Configuration
import io.snabble.sdk.dynamicview.domain.model.DynamicConfig
import io.snabble.sdk.dynamicview.domain.model.ImageItem
import io.snabble.sdk.dynamicview.domain.model.InformationItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.SectionItem
import io.snabble.sdk.dynamicview.domain.model.TextItem
import io.snabble.sdk.dynamicview.domain.model.toPaddingValues
import io.snabble.sdk.dynamicview.theme.ThemeWrapper
import io.snabble.sdk.dynamicview.viewmodel.DynamicViewModel
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.widgets.ImageWidget

@Composable
internal fun DynamicScreen(
    modifier: Modifier,
    dynamicViewModel: DynamicViewModel,
    onAction: OnDynamicAction = dynamicViewModel::sendAction,
) {
    val configState = dynamicViewModel.dynamicConfig.collectAsStateWithLifecycle()
    val config = configState.value ?: return
    DynamicView(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier),
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

@Composable
private fun DynamicScreenPreviewWith(config: DynamicConfig) {
    ThemeWrapper {
        val viewModel: DynamicViewModel = viewModel()
        viewModel.apply { setConfig(config) }
        DynamicScreen(dynamicViewModel = viewModel, modifier = Modifier)
    }
}

@Preview(
    backgroundColor = 0xFFFFFF,
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun HomeScreenPreview() {
    val config = DynamicConfig(
        configuration = Configuration(
            image = R.drawable.home_default_background,
            style = "",
            padding = Padding(16)
        ),
        widgets = listOf(
            TextItem(
                id = "header",
                text = "Willkommen bei Snabble",
                textColor = MaterialTheme.colorScheme.primary.toArgb(),
                textStyle = "title",
                showDisclosure = false,
                padding = Padding(start = 16, top = 16, end = 16, bottom = 0),
            ),
            TextItem(
                id = "body",
                text = "Deine App für Scan and Go!",
                textColor = MaterialTheme.colorScheme.primary.toArgb(),
                textStyle = "body",
                showDisclosure = false,
                padding = Padding(16, 5),
            ),
            TextItem(
                id = "footer",
                text = "Snabble",
                textColor = null,
                textStyle = "footer",
                showDisclosure = false,
                padding = Padding(start = 16, top = 0, end = 16, bottom = 16),
            ),
            InformationItem(
                id = "info",
                text = "Your information",
                image = R.drawable.store_logo,
                padding = Padding(16, 16)
            )
        )
    )
    DynamicScreenPreviewWith(config = config)
}

@Preview(
    backgroundColor = 0xFFFFFF,
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun ProfileScreenPreview() {
    val config = DynamicConfig(
        configuration = Configuration(
            image = null,
            style = "",
            padding = Padding(0)
        ),
        widgets = listOf(
            SectionItem(
                id = "section",
                header = "Profile",
                padding = Padding(0),
                items = listOf(
                    TextItem(
                        id = "1",
                        text = "My Profile",
                        showDisclosure = false,
                        padding = Padding(horizontal = 16, vertical = 5),
                    ),
                    TextItem(
                        id = "1",
                        text = "Settings",
                        showDisclosure = false,
                        padding = Padding(horizontal = 16, vertical = 5),
                    ),
                    TextItem(
                        id = "1",
                        text = "Delete Account",
                        showDisclosure = false,
                        padding = Padding(horizontal = 16, vertical = 5),
                    ),
                ),
            )
        )
    )
    DynamicScreenPreviewWith(config = config)
}
