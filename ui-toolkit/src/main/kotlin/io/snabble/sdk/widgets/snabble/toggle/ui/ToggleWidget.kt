package io.snabble.sdk.widgets.snabble.toggle.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebaslogen.resaca.viewModelScoped
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.ToggleItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.widgets.ToggleWidget
import io.snabble.sdk.widgets.snabble.toggle.viewmodel.ToggleViewModel
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

@Composable
internal fun ToggleWidget(
    modifier: Modifier = Modifier,
    model: ToggleItem,
    viewModel: ToggleViewModel = viewModelScoped(model.key) { KoinProvider.get { parametersOf(model.key) } },
    onAction: OnDynamicAction,
) {
    val isCheckedState = viewModel.toggleState.collectAsStateWithLifecycle()
    ToggleWidget(
        modifier = modifier,
        model = model,
        isChecked = isCheckedState.value,
        onCheckedChange = viewModel::saveToggleState,
        onAction = onAction,
    )
}
