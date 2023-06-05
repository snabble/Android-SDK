package io.snabble.sdk.ui.payment.externalbilling

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.payment.externalbilling.ui.ExternalBillingLoginScreen
import io.snabble.sdk.ui.payment.externalbilling.viewmodel.ExternalBillingViewModel
import io.snabble.sdk.ui.utils.ThemeWrapper

class ExternalBillingFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_external_billing,
    waitForProject = false,
) {

    private val viewModel: ExternalBillingViewModel by viewModels()

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ComposeView>(R.id.external_billing_compose_view).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                ThemeWrapper {
                    ExternalBillingLoginScreen(onSaveClick = { username, password ->
                        viewModel.login(username, password)
                    }, isInputValid = false)
                }
            }
        }
    }

    companion object {

        fun createFragment(): ExternalBillingFragment =
            ExternalBillingFragment()
    }
}
