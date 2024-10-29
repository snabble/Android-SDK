package io.snabble.sdk.ui.payment.creditcard.fiserv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.payment.PaymentMethodMetaDataHelper
import io.snabble.sdk.ui.payment.creditcard.shared.CustomerInfoInputScreen
import io.snabble.sdk.ui.utils.ThemeWrapper
import io.snabble.sdk.ui.utils.serializableExtra

open class FiservInputFragment : Fragment() {

    private lateinit var paymentMethod: String
    private lateinit var projectId: String

    private var savePaymentCredentials: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentMethod =
            arguments?.serializableExtra<String>(FiservInputView.ARG_PAYMENT_TYPE)
                ?: kotlin.run { activity?.onBackPressedDispatcher?.onBackPressed(); return }

        projectId = arguments?.serializableExtra<String>(FiservInputView.ARG_PROJECT_ID)
            ?: kotlin.run { activity?.onBackPressedDispatcher?.onBackPressed(); return }

        savePaymentCredentials = arguments?.getBoolean(FiservInputView.ARG_SAVE_PAYMENT_CREDENTIALS, true) ?: true

        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            PaymentMethodMetaDataHelper(requireContext()).labelFor(PaymentMethod.valueOf(paymentMethod))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setContent {
                FiservScreen(
                    projectId = projectId,
                    paymentMethod = PaymentMethod.valueOf(paymentMethod),
                    savePaymentCredentials = savePaymentCredentials,
                    onBackNavigationClick = { activity?.onBackPressedDispatcher?.onBackPressed(); }
                )
            }
        }
}

@Composable
fun FiservScreen(
    projectId: String,
    paymentMethod: PaymentMethod,
    savePaymentCredentials: Boolean,
    onBackNavigationClick: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: FiservViewModel = viewModel(factory = FiservViewModelFactory(context))
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
                    onBackNavigationClick = onBackNavigationClick
                )
            }

        else -> AndroidView(
            factory = { ctx ->
                FiservInputView(ctx)
                    .apply {
                        setSavePaymentCredentials(savePaymentCredentials)
                        load(
                            projectId,
                            paymentMethod,
                            uiState.formUrl,
                            uiState.deletePreAuthUrl
                        )
                    }
            }
        )
    }
}
