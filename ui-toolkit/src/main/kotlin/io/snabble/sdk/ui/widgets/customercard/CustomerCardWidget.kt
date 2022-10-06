package io.snabble.sdk.ui.widgets.customercard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.domain.CustomerCardItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.ui.DynamicAction
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.theme.properties.Elevation
import io.snabble.sdk.ui.theme.properties.LocalElevation
import io.snabble.sdk.ui.theme.properties.LocalPadding
import io.snabble.sdk.ui.theme.properties.applyElevation
import io.snabble.sdk.ui.theme.properties.applyPadding
import io.snabble.sdk.ui.toInformationItem
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.widgets.InformationWidget
import io.snabble.sdk.ui.widgets.customercard.viewmodel.CustomerCardViewModel
import org.koin.androidx.compose.getViewModel

@Composable
internal fun CustomerCardWidget(
    modifier: Modifier = Modifier,
    customerCardViewModel: CustomerCardViewModel = getViewModel(scope = KoinProvider.scope),
    model: CustomerCardItem,
    onAction: OnDynamicAction,
) {
    @OptIn(ExperimentalLifecycleComposeApi::class)
    val isCardVisibleState = customerCardViewModel.isCustomerCardVisible.collectAsStateWithLifecycle()
    if (isCardVisibleState.value) {
        InformationWidget(
            modifier = modifier,
            model = (model.toInformationItem()),
            onAction = { onAction(DynamicAction(model)) },
        )
    }
}

@Composable
private fun CustomerCard(
    modifier: Modifier = Modifier,
    model: CustomerCardItem,
    onAction: OnDynamicAction,
) {
    InformationWidget(
        modifier = modifier,
        model = (model.toInformationItem()),
        onAction = { onAction(DynamicAction(model)) },
    )
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun CustomerCardPreview() {
    CompositionLocalProvider(
        LocalPadding provides io.snabble.sdk.ui.theme.properties.Padding().applyPadding(),
        LocalElevation provides Elevation().applyElevation()
    ) {
        CustomerCard(
            model = CustomerCardItem(
                id = "an.image",
                text = "Füge deine Kundenkarte hinzu.",
                imageSource = R.drawable.store_logo,
                padding = Padding(start = 8, top = 8, end = 8, bottom = 8),
            ),
            onAction = {}
        )
    }
}
