package io.snabble.sdk.widgets

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.ToggleItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.utils.toPaddingValues
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction

@Composable
fun ToggleWidget(
    modifier: Modifier = Modifier,
    model: ToggleItem,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onAction: OnDynamicAction,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(model.padding.toPaddingValues())
            .then(modifier)
    ) {
        val (label, switch) = createRefs()
        Text(
            text = model.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.constrainAs(label) {
                linkTo(start = parent.start, end = switch.start)
                linkTo(top = parent.top, bottom = parent.bottom)
                width = Dimension.fillToConstraints
            }
        )
        Switch(
            checked = isChecked,
            onCheckedChange = {
                onCheckedChange(it)
                onAction(DynamicAction(model, mapOf("checked" to it)))
            },
            modifier = Modifier.constrainAs(switch) {
                linkTo(start = parent.start, end = parent.end, bias = 1f)
                linkTo(top = parent.top, bottom = parent.bottom)
            }
        )
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun TogglePreview() {
    val state = remember { mutableStateOf(true) }
    ToggleWidget(
        model = ToggleItem(
            id = "setup.toggle",
            text = "Show setup",
            key = "pref.setup.toggle",
            padding = Padding(horizontal = 16),
        ),
        isChecked = state.value,
        onCheckedChange = { state.value = it },
        onAction = {},
    )
}