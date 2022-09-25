package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.domain.InformationItem
import io.snabble.sdk.domain.Padding
import io.snabble.sdk.ui.WidgetClick
import io.snabble.sdk.ui.toPaddingValues
import io.snabble.sdk.ui.toolkit.R

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun InformationWidgetPreview() {
    InformationWidget(
        model = InformationItem(
            id = "an.image",
            text = "Füge deine Kundenkarte hinzu.",
            imageSource = R.drawable.store_logo,
            padding = Padding(start = 16, top = 8, end = 16, bottom = 8),
        ),
        onclick = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun InformationWidget(
    modifier: Modifier = Modifier,
    model: InformationItem,
    onclick: WidgetClick
) {
    rememberRipple()
    Card(
        onClick = { onclick },
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color.White,
        elevation = 4.dp,
        interactionSource = remember {
            MutableInteractionSource()
        }) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(model.padding.toPaddingValues()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (model.imageSource != null) {
                Image(
                    modifier = Modifier
                        .padding(end = 16.dp),
                    contentScale = ContentScale.Fit,
                    painter = painterResource(id = model.imageSource),
                    contentDescription = "",
                )
            }
            Text(text = model.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}