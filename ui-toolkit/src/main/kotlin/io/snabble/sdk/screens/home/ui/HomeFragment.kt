package io.snabble.sdk.screens.home.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.snabble.sdk.screens.home.viewmodel.DynamicHomeViewModel
import io.snabble.sdk.dynamicview.theme.ThemeWrapper

class HomeFragment : Fragment() {

    private val viewModel: DynamicHomeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setContent {
                setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
                ThemeWrapper {
                    DynamicScreen(dynamicViewModel = viewModel)
                }
            }
        }
}
