package io.snabble.sdk.screens.profile.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import io.snabble.sdk.dynamicview.theme.ThemeWrapper
import io.snabble.sdk.dynamicview.ui.DynamicScreen
import io.snabble.sdk.screens.profile.viewmodel.DynamicProfileViewModel
import io.snabble.sdk.utils.getComposeColor

class ProfileFragment : Fragment() {

    private val viewModel: DynamicProfileViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(inflater.context).apply {
            setContent {
                setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
                ThemeWrapper {
                    DynamicScreen(
                        dynamicViewModel = viewModel,
                        modifier = Modifier
                            .background(
                                Color(LocalContext.current.getComposeColor("profile_background")
                                    ?: MaterialTheme.colorScheme.background.toArgb())
                            )
                    )
                }
            }
        }

    override fun onStart() {
        super.onStart()

        val supportActionBar = (context as? AppCompatActivity)?.supportActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
}
