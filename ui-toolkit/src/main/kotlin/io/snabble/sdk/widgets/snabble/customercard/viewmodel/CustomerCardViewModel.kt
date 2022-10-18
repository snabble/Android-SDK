package io.snabble.sdk.widgets.snabble.customercard.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.widgets.snabble.customercard.domain.HasCustomerCardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class CustomerCardViewModel(
    hasCustomerCard: HasCustomerCardUseCase,
) : ViewModel() {

    val isCustomerCardVisible = MutableStateFlow(hasCustomerCard()).asStateFlow()
}
