package io.snabble.sdk.ui.payment.creditcard.datatrans.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ch.datatrans.payment.api.TransactionRegistry
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.Keyguard
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.payment.PaymentMethodMetaDataHelper
import io.snabble.sdk.ui.payment.creditcard.shared.CustomerInfoInputScreen
import io.snabble.sdk.ui.utils.ThemeWrapper
import io.snabble.sdk.ui.utils.serializableExtra
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class DatatransFragment : Fragment() {

    private val viewModel: DatatransViewModel by viewModels { DatatransViewModelFactory(requireContext()) }

    private lateinit var paymentMethod: PaymentMethod

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentMethod = arguments?.serializableExtra<PaymentMethod>(ARG_PAYMENT_TYPE)
            ?: kotlin.run { activity?.onBackPressed(); return }

        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            PaymentMethodMetaDataHelper(requireContext()).labelFor(paymentMethod)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setContent {
                val uiState: UiState = viewModel.uiState.collectAsStateWithLifecycle().value

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
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleEvents()
    }

    private fun handleEvents() {
        lifecycleScope.launch {
            viewModel.event
                .filterNotNull()
                .flowWithLifecycle(lifecycle)
                .collectLatest { event ->
                    when (event) {
                        Event.TransactionFailed -> {
                            val err = when (paymentMethod) {
                                PaymentMethod.TWINT -> R.string.Snabble_Payment_Twint_error
                                PaymentMethod.POST_FINANCE_CARD -> R.string.Snabble_Payment_PostFinanceCard_error
                                else -> R.string.Snabble_Payment_CreditCard_error
                            }

                            Toast.makeText(activity, err, Toast.LENGTH_LONG).show()
                        }

                        is Event.TransActionCreated -> {
                            activity?.let { TransactionRegistry.startTransaction(it, event.transaction) }
                        }

                        is Event.TransActionSucceeded -> activity?.let {
                            Keyguard.unlock(it, object : Keyguard.Callback {
                                override fun success() {
                                    viewModel.saveDatatransToken(
                                        event.datatransToken,
                                        event.datatransToken.token.getDisplayTitle(it)
                                    )
                                }

                                override fun error() {
                                    Toast
                                        .makeText(activity, R.string.Snabble_SEPA_encryptionError, Toast.LENGTH_LONG)
                                        .show()

                                    activity?.onBackPressed()
                                }
                            })
                        }

                        Event.Finish -> activity?.onBackPressed()
                    }
                }
        }
    }

    companion object {

        const val ARG_PROJECT_ID: String = "projectId"
        const val ARG_PAYMENT_TYPE: String = "paymentType"
    }
}
