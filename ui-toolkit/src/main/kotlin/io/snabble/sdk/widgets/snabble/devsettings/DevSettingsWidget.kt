package io.snabble.sdk.widgets.snabble.devsettings

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.snabble.sdk.dynamicview.domain.model.DevSettingsItem
import io.snabble.sdk.dynamicview.domain.model.toPaddingValues
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction

@Composable
fun DevSettingsWidget(
    model: DevSettingsItem,
    modifier: Modifier = Modifier,
    onAction: OnDynamicAction,
    indication: Indication? = rememberRipple(),

    ) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = indication,
            ) {
                onAction(DynamicAction(model))
            }
            .padding(model.padding.toPaddingValues())
            .then(modifier)
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterStart),
            text = model.text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
