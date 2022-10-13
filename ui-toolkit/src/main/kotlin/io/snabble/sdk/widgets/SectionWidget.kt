package io.snabble.sdk.widgets

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.snabble.sdk.dynamicview.domain.model.ButtonItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.SectionItem
import io.snabble.sdk.dynamicview.domain.model.TextItem
import io.snabble.sdk.dynamicview.domain.model.ToggleItem
import io.snabble.sdk.dynamicview.theme.properties.Elevation
import io.snabble.sdk.dynamicview.theme.properties.LocalElevation
import io.snabble.sdk.dynamicview.theme.properties.LocalPadding
import io.snabble.sdk.dynamicview.theme.properties.applyElevation
import io.snabble.sdk.dynamicview.theme.properties.applyPadding
import io.snabble.sdk.dynamicview.theme.properties.padding
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.utils.toPaddingValues
import io.snabble.sdk.widgets.snabble.toggle.ui.ToggleWidget
import io.snabble.sdk.dynamicview.theme.properties.Padding as OuterPadding

@Composable
fun SectionWidget(
    modifier: Modifier = Modifier,
    model: SectionItem,
    onAction: OnDynamicAction,
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
            style = MaterialTheme.typography.titleSmall
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
private fun SectionPreview() {
    CompositionLocalProvider(
        LocalPadding provides OuterPadding().applyPadding(),
        LocalElevation provides Elevation().applyElevation()
    ) {
        SectionWidget(
            model = SectionItem(
                id = "section",
                header = "Profil",
                items = listOf(
                    TextItem(
                        id = "1",
                        text = "Setup",
                        textStyleSource = "body",
                        showDisclosure = false,
                        padding = Padding(horizontal = 16, vertical = 5),
                    ),
                    TextItem(
                        id = "1",
                        text = "Terms",
                        textStyleSource = "body",
                        showDisclosure = false,
                        padding = Padding(horizontal = 16, vertical = 5),
                    ),
                ),
                padding = Padding(0, 0, 0, 0)
            ),
            onAction = {})
    }

}
