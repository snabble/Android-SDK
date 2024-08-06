package io.snabble.sdk.ui.payment.telecash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.payment.CreditCardInputView
import io.snabble.sdk.ui.payment.PaymentMethodMetaDataHelper
import io.snabble.sdk.ui.utils.ThemeWrapper
import io.snabble.sdk.ui.utils.serializableExtra

class CustomerInfoInputFragment : Fragment() {

    private val viewModel: TelecashViewModel by viewModels { TelecashViewModel.Factory }

    private lateinit var paymentMethod: PaymentMethod

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentMethod = arguments?.serializableExtra<PaymentMethod>(CreditCardInputView.ARG_PAYMENT_TYPE)
            ?: kotlin.run { activity?.onBackPressed(); return }

        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            PaymentMethodMetaDataHelper(requireContext()).labelFor(paymentMethod)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setContent {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value // TBI: It's currently only the url

                when {
                    uiState.formUrl == null ->
                        ThemeWrapper {
                            CustomerInfoInputScreen(
                                onErrorProcessed = { viewModel.errorHandled() },
                                isLoading = uiState.isLoading,
                                onSendAction = { viewModel.sendUserData(it) },
                                showError = uiState.showError,
                                onBackNavigationClick = { activity?.onBackPressed() }
                            )
                        }

                    else -> AndroidView(
                        factory = { context ->
                            CreditCardInputView(context)
                                .apply { load(Snabble.checkedInProject.value?.id, paymentMethod, uiState.formUrl) }
                        }
                    )
                }
            }
        }
}
