package io.snabble.sdk.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.widgets.Widget

typealias WidgetClick = (id: String) -> Unit

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun HomeScreenPreview() {
    DynamicView(
        imageRes = R.drawable.snabble_onboarding_step1,
        widgets = listOf(),
        widgetFactory = object : WidgetFactory {},
    )
}

@Composable
fun DynamicView(
    imageRes: Int? = null,
    widgets: List<Widget>,
    widgetFactory: WidgetFactory,
    onClick: WidgetClick? = null,
) {
    if (imageRes != null) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "",
                contentScale = ContentScale.Crop,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(widgets) { widget ->
            widgetFactory.createWidget(widget, onClick)
        }
    }
}

interface WidgetFactory {
    fun createWidget(widget: Widget, click: WidgetClick? = null) {}
}
