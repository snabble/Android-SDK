package io.snabble.sdk.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Magenta
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.snabble.sdk.di.KoinProvider
import io.snabble.sdk.domain.Configuration
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.LocationPermissionItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.domain.ProjectId
import io.snabble.sdk.domain.PurchasesItem
import io.snabble.sdk.domain.Root
import io.snabble.sdk.domain.SeeAllStoresItem
import io.snabble.sdk.domain.StartShoppingItem
import io.snabble.sdk.domain.TextItem
import io.snabble.sdk.home.viewmodel.Error
import io.snabble.sdk.home.viewmodel.Finished
import io.snabble.sdk.home.viewmodel.HomeViewModel
import io.snabble.sdk.home.viewmodel.Loading
import io.snabble.sdk.ui.DynamicView
import io.snabble.sdk.ui.DynamicViewModel
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.toPaddingValues
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.widgets.ImageWidget
import io.snabble.sdk.utils.getComposeColor
import org.koin.androidx.compose.getViewModel

@Composable
private fun Home(
    homeConfig: Root,
    onAction: OnDynamicAction
) {
    DynamicView(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(
                    LocalContext.current.getComposeColor("snabble_background") ?: Magenta.toArgb()
                )
            ),
        contentPadding = homeConfig.configuration.padding.toPaddingValues(),
        background = {
            if (homeConfig.configuration.image != null) {
                ImageWidget(
                    model = ImageItem(
                        "background.image",
                        homeConfig.configuration.image,
                        Padding(all = 0)
                    ),
                    contentScale = ContentScale.Fit,
                    onClick = onAction,
                )
            }
        },
        widgets = homeConfig.widgets,
        onAction = onAction,
    )
}

@Composable
internal fun HomeScreen(
    homeViewModel: HomeViewModel = getViewModel(scope = KoinProvider.scope),
    dynamicViewModel: DynamicViewModel = viewModel(),
) {
    when (val state = homeViewModel.homeState.collectAsState().value) {
        Loading -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        is Finished -> {
            Home(homeConfig = state.root, onAction = dynamicViewModel::sendAction)
        }
        is Error -> {
            Toast.makeText(LocalContext.current, state.e.message, Toast.LENGTH_LONG).show()
        }
    }
}

@Preview(
    backgroundColor = 0xFFFFFF,
    showBackground = true,
    showSystemUi = true,
)
@Composable
private fun HomePreview() {
    Home(
        homeConfig = Root(
            configuration = Configuration(
                image = R.drawable.home_default_background,
                style = "",
                padding = Padding(16)
            ),
            widgets = listOf(
                TextItem(
                    id = "hello.world.text",
                    text = "Willkommen bei Snabble",
                    textColorSource = MaterialTheme.colorScheme.primary.toArgb(),
                    textStyleSource = "header",
                    showDisclosure = false,
                    padding = Padding(start = 16, top = 16, end = 16, bottom = 0),
                ),
                TextItem(
                    id = "title",
                    text = "Deine App für Scan and Go!",
                    textColorSource = MaterialTheme.colorScheme.primary.toArgb(),
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
                ),
                PurchasesItem(
                    id = "last.purchases",
                    projectId = ProjectId("0123"),
                    padding = Padding(0),
                ),
            )
        ),
        onAction = {}
    )
}
