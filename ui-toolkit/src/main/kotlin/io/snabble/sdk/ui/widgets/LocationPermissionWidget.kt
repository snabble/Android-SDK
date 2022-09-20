package io.snabble.sdk.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.domain.LocationPermissionItem
import io.snabble.sdk.ui.WidgetClick
import io.snabble.sdk.ui.toolkit.R


@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun LocationPermissionPreview() {
    LocationPermissionWidget(
        model = LocationPermissionItem("1", 16),
        permissionState = false
    )
}

@Composable
fun LocationPermissionWidget(
    modifier: Modifier = Modifier,
    model: LocationPermissionItem,
    permissionState: Boolean = false,
    onClick: WidgetClick = {},
) {
    if (!permissionState) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = model.padding.dp, vertical = 0.dp)
        ) {
            Button(
                onClick = { onClick(model.id) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(
                        MaterialTheme.colors.primary.toArgb()
                    ),
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = modifier.align(Alignment.Center),
            ) {
                Text(text = stringResource(id = R.string.Snabble_askForPermission))
            }
        }
    }
}