package io.snabble.sdk.widgets.snabble.stores.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebaslogen.resaca.viewModelScoped
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.StartShoppingItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.widgets.StartShopping
import io.snabble.sdk.widgets.snabble.stores.viewmodel.StoresViewModel
import org.koin.core.component.get

@Composable
internal fun StartShoppingWidget(
    modifier: Modifier = Modifier,
    model: StartShoppingItem,
    viewModel: StoresViewModel = viewModelScoped { KoinProvider.get() },
    onAction: OnDynamicAction,
) {
    val isCheckedInState = viewModel.isCheckedInFlow.collectAsStateWithLifecycle()
    if (isCheckedInState.value) {
        StartShopping(
            modifier = modifier.heightIn(48.dp),
            model = model,
            onAction = onAction,
        )
    }
}
