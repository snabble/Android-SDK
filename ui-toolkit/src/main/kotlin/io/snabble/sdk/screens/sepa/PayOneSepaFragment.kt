package io.snabble.sdk.screens.sepa

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.snabble.sdk.dynamicview.theme.ThemeWrapper
import io.snabble.sdk.screens.sepa.ui.PayOneSepaScreen
import io.snabble.sdk.screens.sepa.viewmodel.SepaViewModel

class PayOneSepaFragment : Fragment() {

    private val viewModel: SepaViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setContent {
                val showError = viewModel.showError.collectAsState().value
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                ThemeWrapper {
                    PayOneSepaScreen(
                        saveData = { viewModel.saveData(it) },
                        validateIban = { viewModel.validateIban(it) },
                        ibanIsValid = showError
                    )
                }
            }
        }
}
