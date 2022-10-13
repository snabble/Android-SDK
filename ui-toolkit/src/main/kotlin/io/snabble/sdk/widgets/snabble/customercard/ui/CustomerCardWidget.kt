package io.snabble.sdk.widgets.snabble.customercard.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebaslogen.resaca.viewModelScoped
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.domain.model.CustomerCardItem
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction
import io.snabble.sdk.widgets.CustomerCardWidget
import io.snabble.sdk.widgets.snabble.customercard.viewmodel.CustomerCardViewModel

@Composable
internal fun CustomerCardWidget(
    modifier: Modifier = Modifier,
    customerCardViewModel: CustomerCardViewModel = viewModelScoped { KoinProvider.scope.get() },
    model: CustomerCardItem,
    onAction: OnDynamicAction,
) {
    @OptIn(ExperimentalLifecycleComposeApi::class)
    val isCardVisibleState = customerCardViewModel.isCustomerCardVisible.collectAsStateWithLifecycle()
    if (isCardVisibleState.value) {
        CustomerCardWidget(
            modifier = modifier,
            model = (model),
            onAction = { onAction(DynamicAction(model)) },
        )
    }
}
