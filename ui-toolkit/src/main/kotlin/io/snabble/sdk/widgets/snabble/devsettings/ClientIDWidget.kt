package io.snabble.sdk.widgets.snabble.devsettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.Snabble
import io.snabble.sdk.dynamicview.domain.model.ClientIdItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.toPaddingValues
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction
import io.snabble.sdk.widgets.snabble.devsettings.utils.copyToClipBoard

@Composable
fun ClientIdWidget(
    modifier: Modifier = Modifier,
    model: ClientIdItem,
    onAction: OnDynamicAction,
) {
    val clientId: String = if (!LocalInspectionMode.current) {
        Snabble.clientId.toString()
    } else {
        "1.0"
    }

    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            ) {
                context.copyToClipBoard("Client-ID", clientId)
                onAction(DynamicAction(model))
            }
            .padding(model.padding.toPaddingValues())
            .then(modifier)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Client-ID",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = clientId,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Preview
@Composable
fun ClientIDWidgetPreview() {
    ClientIdWidget(
        model = ClientIdItem(
            id = "a.Client",
            padding = Padding(0)
        ),
        onAction = {}
    )
}
