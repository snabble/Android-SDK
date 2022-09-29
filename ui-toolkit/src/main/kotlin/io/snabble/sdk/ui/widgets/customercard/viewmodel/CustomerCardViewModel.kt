package io.snabble.sdk.ui.widgets.customercard.viewmodel

import androidx.lifecycle.ViewModel
import io.snabble.sdk.usecases.GetCustomerCardInfo

internal class CustomerCardViewModel(
    getCustomerCardInfo: GetCustomerCardInfo,
) : ViewModel() {

    val customerCardVisibilityState =
        getCustomerCardInfo()
}

