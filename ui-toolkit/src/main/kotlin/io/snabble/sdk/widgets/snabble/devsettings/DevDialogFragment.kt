package io.snabble.sdk.widgets.snabble.devsettings

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.dynamicview.theme.ThemeWrapper
import io.snabble.sdk.widgets.snabble.devsettings.usecase.HasEnableDevSettingsUseCaseImpl
import io.snabble.sdk.widgets.snabble.devsettings.viewmodel.DevViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject

class DevDialogFragment : DialogFragment() {

    private val viewModel: DevViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val hasEnableDevSettings: HasEnableDevSettingsUseCaseImpl by KoinProvider.inject()
            hasEnableDevSettings()
                .onEach {
                    if (it) this@DevDialogFragment.dismiss()
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
            setContent {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                ThemeWrapper {
                    DevLoginWidget(
                        dismiss = {
                            viewModel.resetErrorMessage()
                            this@DevDialogFragment.dismiss()
                        },
                        login = {
                            viewModel.onEnableSettingsClick(it)
                        },
                        onPasswordChange = { viewModel.resetErrorMessage() },
                        showError = viewModel.showError.collectAsState().value
                    )
                }
            }
        }
}
