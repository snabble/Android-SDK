package io.snabble.sdk.ui.widgets.customercard.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.usecases.HasCustomerCardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CustomerCardViewModel(
    hasCustomerCard: HasCustomerCardUseCase,
) : ViewModel() {

    val isCustomerCardVisible: StateFlow<Boolean> = MutableStateFlow(hasCustomerCard()).asStateFlow()
}

