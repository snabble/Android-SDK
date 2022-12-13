package io.snabble.sdk.ui.payment.payone.sepa.credentials

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.snabble.sdk.ui.payment.payone.sepa.credentials.viewmodel.SepaViewModel
import io.snabble.sdk.ui.Keyguard
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.payment.payone.sepa.credentials.ui.PayoneSepaScreen
import io.snabble.sdk.ui.utils.KeyguardUtils
import io.snabble.sdk.ui.utils.UIUtils

class PayoneSepaCredentialsFragment : Fragment() {

    private val viewModel: SepaViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val isIbanValid = viewModel.isIbanValid.collectAsState().value
                PayoneSepaScreen(
                    saveData = { data ->
                        if (KeyguardUtils.isDeviceSecure()) {
                            Keyguard.unlock(UIUtils.getHostFragmentActivity(context), object : Keyguard.Callback {

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
                                    }
                                }

                                override fun error() {
                                }
                            })
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
