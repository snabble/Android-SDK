package io.snabble.sdk.ui.payment.telecash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.extensions.xx
import io.snabble.sdk.ui.payment.CreditCardInputView

class UserDetailsFragment : Fragment() {

    val viewModel: TelecashViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setContent {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

                when {
                    uiState.isEmpty() ->
                        UserDetailsScreen(onErrorProcessed = {}, isLoading = false, onSendAction = {
                            viewModel.preuAuth(it)
                        }, showError = false)

                    uiState.isNotEmpty() -> AndroidView(
                        factory = { context ->
                            CreditCardInputView(context)
                                .apply { load(PaymentMethod.VISA, uiState.xx()) }
                        }
                    )

                }
            }
        }
}
