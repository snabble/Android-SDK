package io.snabble.sdk.ui.payment.payone.sepa.credentials

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.Keyguard
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.payment.payone.sepa.credentials.ui.PayoneSepaScreen
import io.snabble.sdk.ui.payment.payone.sepa.credentials.viewmodel.SepaViewModel
import io.snabble.sdk.ui.utils.KeyguardUtils
import io.snabble.sdk.ui.utils.ThemeWrapper
import io.snabble.sdk.ui.utils.UIUtils

open class PayoneSepaInputFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_sepa_input_payone,
    waitForProject = false,
) {

    private val viewModel: SepaViewModel by viewModels()

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActualViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val isIbanValid = viewModel.isIbanValid.collectAsState().value
                ThemeWrapper {
                    PayoneSepaScreen(
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
                        validateIban = { viewModel.validateIban(it) },
                        isIbanValid = isIbanValid
                    )
                }
            }

        }
    }

    private fun finish() {
        context?.let { SnabbleUI.executeAction(it, SnabbleUI.Event.GO_BACK) }
    }
}
