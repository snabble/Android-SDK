package io.snabble.sdk.ui.payment.payone.sepa.form

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.viewmodel.MutableCreationExtras
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.Keyguard
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.checkout.PaymentOriginCandidateHelper.PaymentOriginCandidate
import io.snabble.sdk.ui.payment.payone.sepa.form.ui.PayoneSepaFormScreen
import io.snabble.sdk.ui.payment.payone.sepa.form.viewmodel.PayoneSepaFormViewModel
import io.snabble.sdk.ui.utils.KeyguardUtils
import io.snabble.sdk.ui.utils.ThemeWrapper
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.serializableExtra

open class PayoneSepaFormFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_sepa_input_payone,
    waitForProject = false,
) {

    private val viewModel: PayoneSepaFormViewModel by viewModels(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).also { extras ->
                extras[DEFAULT_ARGS_KEY] = arguments
                    ?.serializableExtra<PaymentOriginCandidate>(ARG_PAYMENT_ORIGIN_CANDIDATE)
                    .let {
                        val ibanNumber = it?.origin?.substring(startIndex = 2)
                        bundleOf(PayoneSepaFormViewModel.ARG_PAYMENT_ORIGIN_CANDIDATE to ibanNumber)
                    }
            }
        }
    ) {
        PayoneSepaFormViewModel.Factory
    }

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActualViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val isIbanValid = viewModel.isIbanValid.collectAsState().value
                val iban = viewModel.ibanNumber.collectAsState().value ?: ""
                ThemeWrapper {
                    PayoneSepaFormScreen(
                        saveData = { data ->
                            if (KeyguardUtils.isDeviceSecure()) {
                                Keyguard.unlock(
                                    UIUtils.getHostFragmentActivity(context),
                                    object : Keyguard.Callback {

                                        override fun success() {
                                            val hasBeenSaved = viewModel.saveData(data = data)
                                            if (!hasBeenSaved) {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "Could not verify payment credentials",
                                                        Toast.LENGTH_LONG
                                                    )
                                                    .show()
                                            } else {
                                                finish()
                                            }
                                        }

                                        override fun error() {
                                            // ignored
                                        }
                                    }
                                )
                            } else {
                                AlertDialog.Builder(context)
                                    .setMessage(R.string.Snabble_Keyguard_requireScreenLock)
                                    .setPositiveButton(R.string.Snabble_ok, null)
                                    .setCancelable(false)
                                    .show()
                            }
                        },
                        ibanNumber = iban,
                        onIbanNumberChange = viewModel::onIbanNumberChange,
                        isIbanValid = isIbanValid,
                    )
                }
            }

        }
    }

    private fun finish() {
        context?.let { SnabbleUI.executeAction(it, SnabbleUI.Event.GO_BACK) }
    }

    companion object {

        const val ARG_PAYMENT_ORIGIN_CANDIDATE = "paymentOriginCandidate"

        fun createFragment(paymentOriginCandidate: PaymentOriginCandidate? = null): PayoneSepaFormFragment =
            PayoneSepaFormFragment().apply {
                arguments = paymentOriginCandidate?.let { bundleOf(ARG_PAYMENT_ORIGIN_CANDIDATE to it) }
            }
    }
}
