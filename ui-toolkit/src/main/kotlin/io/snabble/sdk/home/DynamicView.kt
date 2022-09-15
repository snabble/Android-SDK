package io.snabble.sdk.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.Configuration
import io.snabble.sdk.domain.Image
import io.snabble.sdk.domain.Root
import io.snabble.sdk.domain.Widget
import io.snabble.sdk.ui.ImageWidget
import io.snabble.sdk.ui.toolkit.R

typealias WidgetClick = (id: String) -> Unit

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    DynamicView(
        root = Root(
            configuration = Configuration(
                image = R.drawable.snabble_gps_fixed,
                style = "",
                padding = 8
            ),
            widgets = listOf()
        ),
        widgetFactory = object : WidgetFactory {},
    )
}

@Composable
fun DynamicView(
    root: Root,
    widgetFactory: WidgetFactory,
    onClick: WidgetClick? = null,
) {
    if (root.configuration.image != null) {
        ImageWidget(
            model = Image(0, root.configuration.image, 0),
            contentScale = ContentScale.Crop,
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = root.widgets) { widget ->
            widgetFactory.createWidget(widget, onClick)
        }
    }
}

interface WidgetFactory {
    fun createWidget(widget: Widget, click: WidgetClick? = null) {}
}
