package io.snabble.sdk.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.config.Config
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.widgets.ImageModel
import io.snabble.sdk.widgets.ImageWidget
import io.snabble.sdk.widgets.Widget

typealias WidgetClick = (id: String) -> Unit

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    DynamicView(
        imageRes = R.drawable.snabble_onboarding_step1,
        config = Config(
            backgroundImage = ImageModel(
                0,
                imageSource = "R.drawable.background",
                spacing = 8f
            ),
            widgets = listOf()
        ),
        widgetFactory = object : WidgetFactory {},
    )
}

@Composable
fun DynamicView(
    imageRes: Int? = null,
    config: Config,
    widgetFactory: WidgetFactory,
    onClick: WidgetClick? = null,
) {
    if (imageRes != null) {
        ImageWidget(
            model = config.backgroundImage.copy(spacing = 0f),
            contentScale = ContentScale.Crop,
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = config.widgets) { widget ->
            widgetFactory.createWidget(widget, onClick)
        }
    }
}

interface WidgetFactory {
    fun createWidget(widget: Widget, click: WidgetClick? = null) {}
}
