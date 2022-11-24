package io.snabble.sdk.widgets.snabble.devsettings.login.ui

import android.app.Dialog
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
import io.snabble.sdk.widgets.snabble.devsettings.login.usecase.HasEnabledDevSettingsUseCase
import io.snabble.sdk.widgets.snabble.devsettings.login.viewmodel.DevSettingsLoginViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.inject

class DevSettingsLoginFragment : DialogFragment() {

    private val viewModel: DevSettingsLoginViewModel by activityViewModels()

    private val hasEnableDevSettings: HasEnabledDevSettingsUseCase by KoinProvider.inject()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return ComposeView(inflater.context).apply {
            hasEnableDevSettings()
                .onEach {
                    if (it) this@DevSettingsLoginFragment.dismiss()
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

            setContent {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                ThemeWrapper {
                    DevSettingsLogin(
                        dismiss = {
                            viewModel.resetErrorMessage()
                            this@DevSettingsLoginFragment.dismiss()
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
}
