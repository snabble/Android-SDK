package io.snabble.sdk.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.google.android.material.composethemeadapter3.createMdc3Theme
import io.snabble.sdk.ui.toolkit.R

class HomeFragment : Fragment() {

    private lateinit var composeView: ComposeView

    private val viewModel: HomeViewModel by viewModels(ownerProducer = ::requireActivity)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.snabble_fragment_home, container, false).apply {

        composeView = findViewById(R.id.composable)

        viewModel.fetchHomeConfig(context)

        composeView.setContent {

            when (val state = viewModel.homeState.collectAsState().value) {
                Loading -> {
                    ThemeWrapper {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(50.dp)
                                    .align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                is Finished -> {
                    ViewModelStoreOwnerLocalProvider {
                        ThemeWrapper {
                            HomeScreen(homeConfig = state.root)
                        }
                    }
                }
                is Error -> {
                    Toast.makeText(context, state.e.message, Toast.LENGTH_LONG).show()
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
fun Fragment.ThemeWrapper(content: @Composable () -> Unit) {
    var (colorScheme, typography, shapes) = createMdc3Theme(
        context = LocalContext.current,
        layoutDirection = LayoutDirection.Ltr
    )

    MaterialTheme(
        colorScheme = colorScheme ?: MaterialTheme.colorScheme,
        typography = typography ?: MaterialTheme.typography,
        shapes = shapes ?: MaterialTheme.shapes
    ) {
        content()
    }

}

@Composable
fun Fragment.ViewModelStoreOwnerLocalProvider(content: @Composable () -> Unit) {
    val viewModelStoreOwner = compositionLocalOf<ViewModelStoreOwner> { requireActivity() }
    CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner.current) {
        content()
    }
}