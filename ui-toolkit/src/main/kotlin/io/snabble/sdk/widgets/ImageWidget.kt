package io.snabble.sdk.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import io.snabble.sdk.dynamicview.domain.model.ImageItem
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.utils.toPaddingValues
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction
import io.snabble.sdk.ui.R

@Composable
fun ImageWidget(
    modifier: Modifier = Modifier,
    model: ImageItem,
    contentScale: ContentScale = ContentScale.Fit,
    onAction: OnDynamicAction,
    indication: Indication? = rememberRipple(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(model.padding.toPaddingValues()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (model.image != null) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = indication,
                    ) { onAction(DynamicAction(model)) }
                    .then(modifier),
                contentScale = contentScale,
                painter = painterResource(id = model.image),
                contentDescription = "",
            )
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun ImageWidgetPreview() {
    ImageWidget(
        model = ImageItem(
            id = "an.image",
            image = R.drawable.snabble_ic_payment_success_big,
            padding = Padding(horizontal = 8),
        ),
        onAction = {},
        indication = null
    )
}
