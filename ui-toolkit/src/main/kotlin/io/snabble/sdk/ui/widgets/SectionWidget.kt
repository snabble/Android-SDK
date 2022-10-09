package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.snabble.sdk.domain.ButtonItem
import io.snabble.sdk.domain.SectionItem
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.domain.ToggleItem
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.theme.properties.Elevation
import io.snabble.sdk.ui.theme.properties.LocalElevation
import io.snabble.sdk.ui.theme.properties.LocalPadding
import io.snabble.sdk.ui.theme.properties.Padding
import io.snabble.sdk.ui.theme.properties.applyElevation
import io.snabble.sdk.ui.theme.properties.applyPadding
import io.snabble.sdk.ui.theme.properties.padding
import io.snabble.sdk.ui.toPaddingValues
import io.snabble.sdk.ui.widgets.toggle.ToggleWidget

@Composable
fun SectionWidget(
    modifier: Modifier = Modifier,
    model: SectionItem,
    onAction: OnDynamicAction
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(model.padding.toPaddingValues())
            .then(modifier)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.padding.large),
            text = model.header,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
        Column(Modifier.fillMaxWidth()) {
            for (widget in model.items) {
                when (widget) {
                    is TextItem -> TextWidget(
                        model = widget,
                        onAction = onAction,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                    )

                    is ButtonItem -> ButtonWidget(
                        model = widget,
                        onAction = onAction,
                        modifier = Modifier.heightIn(min = 48.dp)
                    )

                    is ToggleItem -> ToggleWidget(
                        model = widget,
                        onAction = onAction,
                        modifier = Modifier.heightIn(min = 48.dp)
                    )

                    else -> Unit
                }
                Divider(modifier = modifier.fillMaxWidth(), thickness = Dp.Hairline)
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