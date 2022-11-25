package io.snabble.sdk.widgets.snabble.devsettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.snabble.sdk.Snabble
import io.snabble.sdk.dynamicview.domain.model.SwitchEnvironmentItem
import io.snabble.sdk.dynamicview.domain.model.toPaddingValues
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.widgets.MultiValueWidget

@Composable
internal fun EnvironmentWidget(
    modifier: Modifier = Modifier,
    model: SwitchEnvironmentItem,
    onAction: OnDynamicAction,
) {
    val (expanded, onExpandChange) = remember { mutableStateOf(false) }
    val currentEnvironment = "${Snabble.environment}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandChange(!expanded) }
            .padding(model.padding.toPaddingValues())
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = model.text,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = currentEnvironment,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        MultiValueWidget(model = model, onAction = onAction, expanded = expanded, onExpandChange = onExpandChange)
    }
}
