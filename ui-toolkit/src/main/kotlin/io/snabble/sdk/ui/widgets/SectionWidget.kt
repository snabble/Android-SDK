package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.domain.SectionItem
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.domain.ToggleItem
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.Widget
import io.snabble.sdk.ui.theme.properties.Elevation
import io.snabble.sdk.ui.theme.properties.LocalElevation
import io.snabble.sdk.ui.theme.properties.LocalPadding
import io.snabble.sdk.ui.theme.properties.Padding
import io.snabble.sdk.ui.theme.properties.applyElevation
import io.snabble.sdk.ui.theme.properties.applyPadding
import io.snabble.sdk.ui.toPaddingValues

@Composable
fun SectionWidget(
    modifier: Modifier = Modifier,
    model: SectionItem,
    onAction: OnDynamicAction
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(model.padding.toPaddingValues()),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = model.header,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Column(Modifier.fillMaxWidth()) {
            model.items.forEach { widget ->
                Widget(widget = widget, onAction = onAction)
                Divider(modifier = modifier.fillMaxWidth())
            }
        }

    }
}


@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun SectionPreview() {
    CompositionLocalProvider(
        LocalPadding provides Padding().applyPadding(),
        LocalElevation provides Elevation().applyElevation()
    ) {
        SectionWidget(
            model = SectionItem(
                id = "section",
                header = "Profil",
                items = listOf(
                    ToggleItem(
                        id = "setup.toggle",
                        text = "Show setup",
                        key = "pref.setup.toggle",
                        padding = io.snabble.sdk.domain.Padding(horizontal = 16, vertical = 5),
                    ),
                    TextItem(
                        id = "1",
                        text = "Willkommen bei Snabble",
                        textStyleSource = "title",
                        showDisclosure = false,
                        padding = io.snabble.sdk.domain.Padding(horizontal = 16, vertical = 5),
                    ),
                ),
                padding = io.snabble.sdk.domain.Padding(0, 0, 0, 0)
            ),
            onAction = {})
    }

}