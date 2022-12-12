package io.snabble.sdk.ui.payment.payone.sepa.mandate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase.AcceptPayoneSepaMandateUseCase
import io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase.AcceptPayoneSepaMandateUseCaseImpl
import io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase.DenyPayoneSepaMandateUseCase
import io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase.DenyPayoneSepaMandateUseCaseImpl
import io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase.FetchPayoneSepaMandateUseCase
import io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase.FetchPayoneSepaMandateUseCaseImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SepaMandateViewModel(
    private val fetchSepaMandate: FetchPayoneSepaMandateUseCase = FetchPayoneSepaMandateUseCaseImpl(),
    private val acceptSepaMandate: AcceptPayoneSepaMandateUseCase = AcceptPayoneSepaMandateUseCaseImpl(),
    private val denySepaMandate: DenyPayoneSepaMandateUseCase = DenyPayoneSepaMandateUseCaseImpl(),
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

    fun accept(hasUserAccepted: Boolean) {
        viewModelScope.launch {
            _mandateFlow.value = Loading
            val isSuccessfullyProcessed = if (hasUserAccepted) {
                acceptSepaMandate()
            } else {
                denySepaMandate()
                false
            }
            _mandateFlow.value = if (isSuccessfullyProcessed) {
                MandateAccepted
            } else {
                AcceptingMandateFailed
            }
        }
    }
}

internal sealed interface UiState
object Loading : UiState
data class Mandate(val mandateHtml: String) : UiState
object MandateAccepted : UiState
object LoadingMandateFailed : UiState
object AcceptingMandateFailed : UiState
