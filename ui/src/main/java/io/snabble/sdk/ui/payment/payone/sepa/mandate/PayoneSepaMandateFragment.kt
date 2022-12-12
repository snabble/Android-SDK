package io.snabble.sdk.ui.payment.payone.sepa.mandate

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.ui.payment.payone.sepa.mandate.ui.PayoneSepaMandateScreen
import io.snabble.sdk.ui.payment.payone.sepa.mandate.viewmodel.SepaMandateViewModel

class PayoneSepaMandateFragment : DialogFragment() {

    private val viewModel: SepaMandateViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))

        return ComposeView(inflater.context).apply {
            setContent {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                @OptIn(ExperimentalLifecycleComposeApi::class)
                val state = viewModel.mandateFlow.collectAsStateWithLifecycle()

//                ThemeWrapper {
                PayoneSepaMandateScreen(
                    state = state.value,
                    onAccepted = { viewModel.accept(hasUserAccepted = true) },
                    onDenied = { this@PayoneSepaMandateFragment.dismiss() },
                    onSuccessAction = { this@PayoneSepaMandateFragment.dismiss() },
                    onErrorAction = { this@PayoneSepaMandateFragment.dismiss() }
                )
//                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        viewModel.accept(hasUserAccepted = false)
    }
}
