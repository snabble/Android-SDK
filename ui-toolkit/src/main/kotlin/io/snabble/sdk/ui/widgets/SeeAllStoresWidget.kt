package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.domain.SeeAllStoresItem
import io.snabble.sdk.ui.AppTheme
import io.snabble.sdk.ui.WidgetClick
import io.snabble.sdk.ui.toolkit.R


@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun SeeAllStoresPreview() {
    Column(Modifier.fillMaxSize()) {
        SeeAllStoresWidget(
            model = SeeAllStoresItem("1", 16),
            checkinState = true
        )
        SeeAllStoresWidget(
            model = SeeAllStoresItem("1", 16),
            checkinState = false
        )
    }
}

@Composable
fun SeeAllStoresWidget(
    modifier: Modifier = Modifier,
    model: SeeAllStoresItem,
    checkinState: Boolean,
    onClick: WidgetClick = {},
) {

    if (!checkinState) {
        ButtonWidget(
            model = model,
            text = stringResource(id = R.string.Snabble_DynamicStack_Shop_show)
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = model.padding.dp, vertical = 0.dp)
                .clickable { onClick(model.id) },
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(id = R.string.Snabble_DynamicStack_Shop_show),
                color = AppTheme.colors.snabble_primaryColor
            )
        }
    }
}
