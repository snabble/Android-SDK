package io.snabble.sdk.ui.payment.telecash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.telecash.data.TelecashRepositoryImpl
import io.snabble.sdk.ui.payment.telecash.domain.CustomerInfo
import io.snabble.sdk.ui.payment.telecash.domain.TelecashRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class TelecashViewModel(
    private val telecashRepo: TelecashRepository = TelecashRepositoryImpl(),
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun sendUserData(customerInfo: CustomerInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val paymentMethod = savedStateHandle.get<PaymentMethod>("paymentType") ?: return@launch
            val result = telecashRepo.sendUserData(customerInfo, paymentMethod)
            result.onSuccess { info ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        formUrl = info.formUrl,
                        deletePreAuthUrl = it.deletePreAuthUrl
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, showError = true) }
            }
        }
    }

    fun errorHandled() {
        _uiState.update { it.copy(showError = false) }
    }

    companion object {

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                TelecashViewModel(savedStateHandle = savedStateHandle)
            }
        }
    }
}

data class UiState(
    val isLoading: Boolean = false,
    val formUrl: String? = null,
    val deletePreAuthUrl: String? = null,
    val showError: Boolean = false
)
