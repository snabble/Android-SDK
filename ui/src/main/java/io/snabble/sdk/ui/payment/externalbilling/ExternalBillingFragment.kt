package io.snabble.sdk.ui.payment.externalbilling

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.payment.externalbilling.ui.ExternalBillingLoginScreen
import io.snabble.sdk.ui.payment.externalbilling.viewmodel.Error
import io.snabble.sdk.ui.payment.externalbilling.viewmodel.ExternalBillingViewModel
import io.snabble.sdk.ui.payment.externalbilling.viewmodel.Processing
import io.snabble.sdk.ui.payment.externalbilling.viewmodel.Success
import io.snabble.sdk.ui.utils.ThemeWrapper

open class ExternalBillingFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_external_billing,
    waitForProject = false,
) {

    private val viewModel: ExternalBillingViewModel by viewModels()

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActualViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.external_billing_compose_view).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
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
                        SnabbleUI.executeAction(context, SnabbleUI.Event.GO_BACK)
                    }
                }

                val title = stringResource(id = R.string.Snabble_Payment_ExternalBilling_title)

                ThemeWrapper {
                    ExternalBillingLoginScreen(
                        onSaveClick = { username, password ->
                            viewModel.login(title, username, password)
                        },
                        isInputValid = false,
                        errorMessage = errorMessage.value,
                        onFocusChanged = {
                            if (state.value != Processing) viewModel.typing()
                        }
                    )
                }
            }
        }
    }

    companion object {

        fun createFragment(): ExternalBillingFragment =
            ExternalBillingFragment()
    }
}
