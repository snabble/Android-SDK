package io.snabble.sdk.widgets.snabble.onboardingtoggle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.snabble.sdk.dynamicview.domain.model.ToggleItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.widgets.snabble.toggle.ui.ToggleWidget

@Composable
fun OnboardingToggle(
    modifier: Modifier = Modifier,
    model: ToggleItem,
    onAction: OnDynamicAction,
) {
    ToggleWidget(modifier = modifier, model = model, onAction = onAction)
}
