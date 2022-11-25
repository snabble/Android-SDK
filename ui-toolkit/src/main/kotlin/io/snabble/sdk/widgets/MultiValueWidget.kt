package io.snabble.sdk.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.dynamicview.domain.model.SwitchEnvironmentItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction

@Composable
internal fun MultiValueWidget(
    modifier: Modifier = Modifier,
    model: SwitchEnvironmentItem,
    onAction: OnDynamicAction,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .then(modifier)
    ) {
        IconButton(onClick = { onExpandChange(!expanded) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "", tint = MaterialTheme.colorScheme.onSurface)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(!expanded) },
        ) {
            model.values.forEach {
                DropdownMenuItem(
                    text = { Text(text = it.text) },
                    onClick = {
                        onExpandChange(!expanded)
                        onAction(DynamicAction(widget = model, info = mapOf("selection" to it.id)))
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun MultiValueWidgetPreview() {
    val (expanded, onExpandChange) = remember { mutableStateOf(false) }
    MultiValueWidget(
        model = SwitchEnvironmentItem(
            id = "",
            text = "Environment",
            values = listOf(
                SwitchEnvironmentItem.Value(id = "prod", text = "Production"),
                SwitchEnvironmentItem.Value(id = "stag", text = "Staging"),
                SwitchEnvironmentItem.Value(id = "dev", text = "Testing"),
            )
        ),
        onAction = {},
        expanded = expanded,
        onExpandChange = onExpandChange
    )
}
