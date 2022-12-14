package io.snabble.sdk.ui.payment.payone.sepa.mandate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase.AbortPayoneSepaMandateProcessUseCase
import io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase.AbortPayoneSepaMandateProcessUseCaseImpl
import io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase.AcceptPayoneSepaMandateUseCase
import io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase.AcceptPayoneSepaMandateUseCaseImpl
import io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase.FetchPayoneSepaMandateUseCase
import io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase.FetchPayoneSepaMandateUseCaseImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SepaMandateViewModel(
    private val fetchSepaMandate: FetchPayoneSepaMandateUseCase = FetchPayoneSepaMandateUseCaseImpl(),
    private val acceptSepaMandate: AcceptPayoneSepaMandateUseCase = AcceptPayoneSepaMandateUseCaseImpl(),
    private val abortMandateProcess: AbortPayoneSepaMandateProcessUseCase = AbortPayoneSepaMandateProcessUseCaseImpl(),
) : ViewModel() {

    private var _mandateFlow = MutableStateFlow<UiState>(Loading)
    internal val mandateFlow = _mandateFlow.asStateFlow()

    init {
        fetchMandate()
    }

    private fun fetchMandate() {
        val html = fetchSepaMandate()
        _mandateFlow.value = if (html != null) {
            Mandate(mandateHtml = html)
        } else {
            LoadingMandateFailed
        }
    }

    fun accept() {
        viewModelScope.launch {
            _mandateFlow.value = Loading
            val isSuccessfullyProcessed = acceptSepaMandate()
            _mandateFlow.value = if (isSuccessfullyProcessed) {
                MandateAccepted
            } else {
                AcceptingMandateFailed
            }
        }
    }

    fun abort() {
        abortMandateProcess()
    }
}

internal sealed interface UiState
object Loading : UiState
data class Mandate(val mandateHtml: String) : UiState
data class AcceptingMandate(val mandateHtml: String) : UiState
object MandateAccepted : UiState
object LoadingMandateFailed : UiState
object AcceptingMandateFailed : UiState
