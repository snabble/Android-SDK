package io.snabble.sdk.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import io.snabble.sdk.domain.Configuration
import io.snabble.sdk.domain.LocationPermissionItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.Root
import io.snabble.sdk.domain.SeeAllStoresItem
import io.snabble.sdk.domain.StartShoppingItem
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.ui.AppTheme
import io.snabble.sdk.ui.toolkit.R

class HomeFragment : Fragment() {
    private lateinit var composeView: ComposeView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.snabble_fragment_home, container, false).apply {

        composeView = findViewById(R.id.composable)
        composeView.setContent {
            HomeScreen(
                homeConfig = Root(
                    configuration = Configuration(
                        image = R.drawable.home_default_background,
                        style = "",
                        padding = Padding(horizontal = 8, vertical = 0),
                    ),
                    widgets = listOf(
                        TextItem(
                            id = "hello.world.text",
                            text = "Willkommen bei Snabble",
                            textColorSource = AppTheme.colors.snabble_primaryColor.toArgb(),
                            textStyleSource = "header",
                            showDisclosure = false,
                            padding = Padding(start = 16, top = 16, end = 16, bottom = 0),
                        ),
                        TextItem(
                            id = "title",
                            text = "Deine App für Scan and Go!",
                            textColorSource = AppTheme.colors.snabble_textColorDark.toArgb(),
                            textStyleSource = "body",
                            showDisclosure = false,
                            padding = Padding(16, 0),
                        ),
                        TextItem(
                            id = "brand",
                            text = "Snabble",
                            textColorSource = null,
                            textStyleSource = "footer",
                            showDisclosure = false,
                            padding = Padding(start = 16, top = 10, end = 16, bottom = 0),
                        ),
                        StartShoppingItem(
                            id = "start",
                            padding = Padding(start = 16, top = 5, end = 16, bottom = 5),
                        ),
                        SeeAllStoresItem(
                            id = "stores",
                            padding = Padding(start = 16, top = 5, end = 16, bottom = 5),
                        ),
                        LocationPermissionItem(
                            id = "location",
                            padding = Padding(start = 16, top = 5, end = 16, bottom = 5),
                        )
                    )
                ),
            )
        }
    }

    override fun onStart() {
        super.onStart()

        val supportActionBar = (context as? AppCompatActivity)?.supportActionBar
        supportActionBar?.hide()
    }

}
