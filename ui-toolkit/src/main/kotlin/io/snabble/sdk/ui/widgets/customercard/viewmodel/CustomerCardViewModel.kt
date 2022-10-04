package io.snabble.sdk.ui.widgets.customercard.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.usecases.GetCustomerCardInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CustomerCardViewModel(
    getCustomerCardInfo: GetCustomerCardInfo,
) : ViewModel() {

    val isCustomerCardVisible: StateFlow<Boolean> =
        MutableStateFlow(getCustomerCardInfo()).asStateFlow()
}

