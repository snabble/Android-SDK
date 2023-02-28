package io.snabble.sdk.ui.payment.payone.sepa.mandate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.ui.payment.payone.sepa.mandate.ui.PayoneSepaMandateScreen
import io.snabble.sdk.ui.payment.payone.sepa.mandate.viewmodel.SepaMandateViewModel
import io.snabble.sdk.ui.utils.ThemeWrapper
import io.snabble.sdk.utils.Logger

open class PayoneSepaMandateFragment : DialogFragment() {

    private val viewModel: SepaMandateViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(inflater.context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        setContent {
            val uiState = viewModel.mandateFlow.collectAsStateWithLifecycle().value

            ThemeWrapper {
                PayoneSepaMandateScreen(
                    state = uiState,
                    onAccepted = { viewModel.accept() },
                    onDenied = { viewModel.abort() },
                    onSuccessAction = { Logger.d("PAYONE SEPA Mandate has been accepted.") },
                    onErrorAction = { viewModel.abort() }
                )
            }
        }
    }
}
