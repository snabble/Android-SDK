package io.snabble.sdk.home

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.snabble.sdk.home.viewmodel.DynamicHomeViewModel
import io.snabble.sdk.ui.theme.ThemeWrapper
import io.snabble.sdk.ui.toolkit.R

class HomeFragment : Fragment(R.layout.snabble_fragment_home) {

    private val viewModel: DynamicHomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ComposeView>(R.id.compose_view).apply {
            setContent {
                ThemeWrapper {
                    // ViewModelStoreOwnerLocalProvider {
                    DynamicScreen(dynamicViewModel = viewModel)
                    // }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val supportActionBar = (context as? AppCompatActivity)?.supportActionBar
        supportActionBar?.hide()
    }
}

@Composable
internal fun Fragment.ViewModelStoreOwnerLocalProvider(content: @Composable () -> Unit) {
    val viewModelStoreOwner = compositionLocalOf<ViewModelStoreOwner> { requireActivity() }
    CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner.current) {
        content()
    }
}
