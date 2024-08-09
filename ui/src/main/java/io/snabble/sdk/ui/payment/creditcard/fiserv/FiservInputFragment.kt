package io.snabble.sdk.ui.payment.creditcard.fiserv

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
import io.snabble.sdk.ui.payment.PaymentMethodMetaDataHelper
import io.snabble.sdk.ui.payment.creditcard.shared.CustomerInfoInputScreen
import io.snabble.sdk.ui.utils.ThemeWrapper
import io.snabble.sdk.ui.utils.serializableExtra

open class FiservInputFragment : Fragment() {

    private val viewModel: FiservViewModel by viewModels { FiservViewModelFactory(requireContext()) }

    private lateinit var paymentMethod: PaymentMethod

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentMethod =
            arguments?.serializableExtra<PaymentMethod>(FiservInputView.ARG_PAYMENT_TYPE)
                ?: kotlin.run { activity?.onBackPressed(); return }

        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            PaymentMethodMetaDataHelper(requireContext()).labelFor(paymentMethod)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setContent {
                val uiState: UiState = viewModel.uiState.collectAsStateWithLifecycle().value

                when {
                    uiState.formUrl == null ->
                        ThemeWrapper {
                            CustomerInfoInputScreen(
                                onErrorProcessed = { viewModel.errorHandled() },
                                isLoading = uiState.isLoading,
                                onSendAction = { viewModel.sendUserData(it) },
                                showError = uiState.showError,
                                countryItems = uiState.countryItems,
                                onBackNavigationClick = { activity?.onBackPressed() }
                            )
                        }

                    else -> AndroidView(
                        factory = { context ->
                            FiservInputView(context)
                                .apply {
                                    load(
                                        Snabble.checkedInProject.value?.id,
                                        paymentMethod,
                                        uiState.formUrl,
                                        uiState.deletePreAuthUrl
                                    )
                                }
                        }
                    )
                }
            }
        }
}
