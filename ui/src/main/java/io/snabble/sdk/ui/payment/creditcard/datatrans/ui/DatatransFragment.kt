package io.snabble.sdk.ui.payment.creditcard.datatrans.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.PaymentMethodMetaDataHelper
import io.snabble.sdk.ui.payment.creditcard.fiserv.FiservInputView
import io.snabble.sdk.ui.payment.creditcard.shared.CustomerInfoInputScreen
import io.snabble.sdk.ui.utils.ThemeWrapper
import io.snabble.sdk.ui.utils.serializableExtra

class DatatransFragment : Fragment() {

    private val viewModel: DatatransViewModel by viewModels { DatatransViewModelFactory(requireContext()) }

    private lateinit var paymentMethod: PaymentMethod

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentMethod =
            arguments?.serializableExtra<PaymentMethod>(FiservInputView.ARG_PAYMENT_TYPE)
                ?: kotlin.run { activity?.onBackPressed(); return } // TBI: Change to Datatrans arg

        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            PaymentMethodMetaDataHelper(requireContext()).labelFor(paymentMethod)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setContent {
                val uiState: UiState = viewModel.uiState.collectAsStateWithLifecycle().value

                ThemeWrapper {
                    when {
                        uiState.mobileToken == null -> {
                            //TBI: Rewrite so that we can provide Customerinfo (datatrans need the phonenumber as sperarate instance see CustomerinfoDto)
                            CustomerInfoInputScreen(
                                onErrorProcessed = { viewModel.errorHandled() },
                                isLoading = uiState.isLoading,
                                onSendAction = { viewModel.sendUserData(it) },
                                showError = uiState.showError,
                                countryItems = uiState.countryItems,
                                onBackNavigationClick = { activity?.onBackPressed() }
                            )
                        }

                        else -> {
                            // TBI: start datatrans transaction
                        }
                    }
                }
            }
        }
}
