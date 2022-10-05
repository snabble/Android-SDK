package io.snabble.sdk.profil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.snabble.sdk.domain.ImageItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.ui.DynamicView
import io.snabble.sdk.ui.DynamicViewModel
import io.snabble.sdk.ui.OnDynamicAction
import io.snabble.sdk.ui.toPaddingValues
import io.snabble.sdk.ui.widgets.ImageWidget
import io.snabble.sdk.utils.getComposeColor

@Composable
internal fun ProfilScreen(
    dynamicViewModel: DynamicViewModel,
    onAction: OnDynamicAction = dynamicViewModel::sendAction,
) {
    val configState = dynamicViewModel.dynamicConfig.collectAsState()
    val config = configState.value ?: return
    DynamicView(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(LocalContext.current.getComposeColor("snabble_background") ?: Color.Magenta.toArgb())
            ),
        contentPadding = config.configuration.padding.toPaddingValues(),
        background = {
            if (config.configuration.image != null) {
                ImageWidget(
                    model = ImageItem(
                        "background.image",
                        config.configuration.image,
                        Padding(all = 0)
                    ),
                    contentScale = ContentScale.Fit,
                    onAction = onAction,
                )
            }
        },
        widgets = config.widgets,
        onAction = onAction,
    )
}