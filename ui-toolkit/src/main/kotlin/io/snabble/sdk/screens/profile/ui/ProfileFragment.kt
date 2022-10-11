package io.snabble.sdk.screens.profile.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.snabble.sdk.dynamicview.theme.ThemeWrapper
import io.snabble.sdk.screens.home.ui.DynamicScreen
import io.snabble.sdk.screens.profile.viewmodel.DynamicProfileViewModel

class ProfileFragment : Fragment() {

    private val viewModel: DynamicProfileViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setContent {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                ThemeWrapper {
                    DynamicScreen(dynamicViewModel = viewModel)
                }
            }
        }

    override fun onStart() {
        super.onStart()

        val supportActionBar = (context as? AppCompatActivity)?.supportActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
}
