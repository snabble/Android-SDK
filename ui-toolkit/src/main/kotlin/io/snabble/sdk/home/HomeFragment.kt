package io.snabble.sdk.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.snabble.sdk.ui.toolkit.R

class HomeFragment : Fragment() {

    private lateinit var composeView: ComposeView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.snabble_fragment_home, container, false).apply {

        composeView = findViewById(R.id.composable)

        composeView.setContent {
            ViewModelStoreOwnerLocalProvider {
                HomeScreen()
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
