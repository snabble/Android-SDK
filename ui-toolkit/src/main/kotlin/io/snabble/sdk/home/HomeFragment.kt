package io.snabble.sdk.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.snabble.sdk.config.ConfigFileProviderImpl
import io.snabble.sdk.config.ConfigRepository
import io.snabble.sdk.data.RootDto
import io.snabble.sdk.domain.ConfigMapperImpl
import io.snabble.sdk.ui.toolkit.R
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class HomeFragment : Fragment() {
    private lateinit var composeView: ComposeView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.snabble_fragment_home, container, false).apply {

        composeView = findViewById(R.id.composable)

        val repo = ConfigRepository(
            ConfigFileProviderImpl(resources.assets),
            Json
        )

        lifecycleScope.launch {
            val rootDto = repo.getConfig<RootDto>("homeConfig.json")
            val root = ConfigMapperImpl(requireContext()).mapTo(rootDto)
            Log.d("TAG", "onCreateView: $root")
            composeView.setContent {
                HomeScreen(homeConfig = root)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val supportActionBar = (context as? AppCompatActivity)?.supportActionBar
        supportActionBar?.hide()
    }

}
