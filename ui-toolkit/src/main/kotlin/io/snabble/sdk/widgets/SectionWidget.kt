package io.snabble.sdk.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.AppUserIdItem
import io.snabble.sdk.dynamicview.domain.model.ButtonItem
import io.snabble.sdk.dynamicview.domain.model.ClientIdItem
import io.snabble.sdk.dynamicview.domain.model.DevSettingsItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.SectionItem
import io.snabble.sdk.dynamicview.domain.model.SwitchEnvironmentItem
import io.snabble.sdk.dynamicview.domain.model.TextItem
import io.snabble.sdk.dynamicview.domain.model.ToggleItem
import io.snabble.sdk.dynamicview.domain.model.VersionItem
import io.snabble.sdk.dynamicview.domain.model.toPaddingValues
import io.snabble.sdk.dynamicview.theme.properties.Elevation
import io.snabble.sdk.dynamicview.theme.properties.LocalElevation
import io.snabble.sdk.dynamicview.theme.properties.LocalPadding
import io.snabble.sdk.dynamicview.theme.properties.applyElevation
import io.snabble.sdk.dynamicview.theme.properties.applyPadding
import io.snabble.sdk.dynamicview.theme.properties.padding
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.utils.isNotNullOrBlank
import io.snabble.sdk.widgets.snabble.devsettings.AppUserIdWidget
import io.snabble.sdk.widgets.snabble.devsettings.ClientIdWidget
import io.snabble.sdk.widgets.snabble.devsettings.DevSettingsWidget
import io.snabble.sdk.widgets.snabble.devsettings.EnvironmentWidget
import io.snabble.sdk.widgets.snabble.devsettings.login.usecase.HasEnabledDevSettingsUseCase
import io.snabble.sdk.widgets.snabble.toggle.ui.ToggleWidget
import io.snabble.sdk.widgets.snabble.version.ui.VersionWidget
import org.koin.core.component.inject
import io.snabble.sdk.dynamicview.theme.properties.Padding as OuterPadding

@Composable
fun SectionWidget(
    modifier: Modifier = Modifier,
    model: SectionItem,
    onAction: OnDynamicAction,
) {
    val hasEnableDevSettings: HasEnabledDevSettingsUseCase by KoinProvider.inject()
    val devSettingsEnabled = hasEnableDevSettings().collectAsStateWithLifecycle(initialValue = false)

    val items = if (!devSettingsEnabled.value) {
        model.items.filterNot { it is DevSettingsItem }
    } else {
        model.items
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(model.padding.toPaddingValues())
            .then(modifier)
    ) {
        if (model.header.isNotNullOrBlank()) {
            SectionHeader(title = model.header)
            Spacer(modifier = Modifier.height(MaterialTheme.padding.default))
        }
        Column(Modifier.fillMaxWidth()) {
            for (widget in items) {
                when (widget) {
                    is AppUserIdItem -> AppUserIdWidget(
                        model = widget,
                        onAction = onAction,
                        modifier = Modifier.heightIn(min = 36.dp)
                    )

                    is ButtonItem -> ButtonWidget(
                        model = widget,
                        onAction = onAction,
                        modifier = Modifier.heightIn(min = 36.dp)
                    )

                    is ClientIdItem -> ClientIdWidget(
                        model = widget,
                        onAction = onAction,
                        modifier = Modifier.heightIn(min = 36.dp)
                    )

                    is DevSettingsItem -> DevSettingsWidget(
                        model = widget,
                        onAction = onAction,
                        modifier = Modifier.heightIn(min = 36.dp)
                    )

                    is TextItem -> TextWidget(
                        model = widget,
                        onAction = onAction,
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                    )

                    is ToggleItem -> ToggleWidget(
                        model = widget,
                        onAction = onAction,
                        modifier = Modifier.heightIn(min = 36.dp)
                    )

                    is VersionItem -> VersionWidget(
                        model = widget,
                        onAction = onAction,
                        modifier = Modifier.heightIn(min = 36.dp)
                    )

                    is SwitchEnvironmentItem -> EnvironmentWidget(
                        model = widget,
                        onAction = onAction,
                        modifier = Modifier.heightIn(min = 36.dp)
                    )

                    else -> Unit // Do nothing
                }
                HorizontalDivider(modifier = modifier.fillMaxWidth(), thickness = Dp.Hairline)
            }
        }
    }
}

@Composable
fun SectionHeader(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MaterialTheme.padding.large)
            .then(modifier),
        text = title,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        style = MaterialTheme.typography.titleSmall
    )
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
                        textStyle = "body",
                        showDisclosure = false,
                        padding = Padding(horizontal = 16, vertical = 5),
                    ),
                    TextItem(
                        id = "1",
                        text = "Terms",
                        textStyle = "body",
                        showDisclosure = false,
                        padding = Padding(horizontal = 16, vertical = 5),
                    ),
                ),
                padding = Padding(0, 0, 0, 0)
            ),
            onAction = {})
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun HeaderlessSectionPreview() {
    CompositionLocalProvider(
        LocalPadding provides OuterPadding().applyPadding(),
        LocalElevation provides Elevation().applyElevation()
    ) {
        SectionWidget(
            model = SectionItem(
                id = "section",
                header = "",
                items = listOf(
                    TextItem(
                        id = "1",
                        text = "Setup",
                        textStyle = "body",
                        showDisclosure = false,
                        padding = Padding(horizontal = 16, vertical = 5),
                    ),
                    TextItem(
                        id = "1",
                        text = "Terms",
                        textStyle = "body",
                        showDisclosure = false,
                        padding = Padding(horizontal = 16, vertical = 5),
                    ),
                ),
                padding = Padding(0, 0, 0, 0)
            ),
            onAction = {})
    }
}
