package io.snabble.sdk.ui.payment.externalbilling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.snabble.sdk.config.ProjectId
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.payment.externalbilling.ui.ExternalBillingLoginScreen
import io.snabble.sdk.ui.payment.externalbilling.viewmodel.Error
import io.snabble.sdk.ui.payment.externalbilling.viewmodel.ExternalBillingViewModel
import io.snabble.sdk.ui.payment.externalbilling.viewmodel.Processing
import io.snabble.sdk.ui.payment.externalbilling.viewmodel.Success
import io.snabble.sdk.ui.utils.ThemeWrapper

@Composable
fun PaymentExternalBillingScreen(
    modifier: Modifier = Modifier,
    projectId: ProjectId?,
    viewModel: ExternalBillingViewModel = viewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage = remember {
        mutableStateOf("")
    }

    when (val it = state.value) {
        is Error -> {
            if (it.message == "400" || it.message == "null") {
                errorMessage.value =
                    stringResource(id = R.string.Snabble_Payment_ExternalBilling_Error_badRequest)
            } else {
                errorMessage.value =
                    stringResource(id = R.string.Snabble_Payment_ExternalBilling_Error_wrongCredentials)
            }
        }

        Processing -> {
            errorMessage.value = ""
        }

        Success -> {
            SnabbleUI.executeAction(LocalContext.current, SnabbleUI.Event.GO_BACK)
        }
    }

    val idDescriptor = stringResource(id = R.string.Snabble_Payment_ExternalBilling_username)

    ThemeWrapper {
        ExternalBillingLoginScreen(
            onSaveClick = { username, password ->
                projectId?.let {
                    viewModel.login(idDescriptor, username, password, it.id)
                }
            },
            isInputValid = false,
            errorMessage = errorMessage.value,
            onFocusChanged = {
                if (state.value != Processing) viewModel.typing()
            }
        )
    }
}
