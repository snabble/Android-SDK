package io.snabble.sdk.widgets.snabble.stores.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebaslogen.resaca.viewModelScoped
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.SeeStoresItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.widgets.AllStoresWidget
import io.snabble.sdk.widgets.snabble.stores.viewmodel.StoresViewModel
import org.koin.core.component.get

@Composable
internal fun SeeAllStoresWidget(
    modifier: Modifier = Modifier,
    model: SeeStoresItem,
    viewModel: StoresViewModel = viewModelScoped { KoinProvider.get() },
    onAction: OnDynamicAction,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        val isCheckedInState = viewModel.isCheckedInFlow.collectAsStateWithLifecycle()
        AllStoresWidget(
            modifier = modifier.heightIn(48.dp),
            model = model,
            isChecked = isCheckedInState.value,
            onAction = onAction
        )
    }
}
