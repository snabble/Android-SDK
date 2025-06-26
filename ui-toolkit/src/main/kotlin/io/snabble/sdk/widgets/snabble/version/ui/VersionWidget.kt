package io.snabble.sdk.widgets.snabble.version.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.Snabble
import io.snabble.sdk.dynamicview.domain.model.Padding
import io.snabble.sdk.dynamicview.domain.model.VersionItem
import io.snabble.sdk.dynamicview.domain.model.toPaddingValues
import io.snabble.sdk.dynamicview.ui.OnDynamicAction
import io.snabble.sdk.dynamicview.viewmodel.DynamicAction

@Composable
fun VersionWidget(
    modifier: Modifier = Modifier,
    model: VersionItem,
    onAction: OnDynamicAction,
) {
    val appVersion: String
    val sdkVersion: String

    if (!LocalInspectionMode.current) {
        appVersion = provideAppVersion(LocalContext.current)
        sdkVersion = Snabble.version
    } else {
        appVersion = "1.0"
        sdkVersion = "0.10.0"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                onAction(DynamicAction(model))
            }
            .padding(model.padding.toPaddingValues())
            .then(modifier)
    ) {
        Text(
            text = "Version",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$appVersion, SDK $sdkVersion",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun provideAppVersion(context: Context): String {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    return packageInfo.versionName ?: ""
}

@Preview
@Composable
fun VersionWidgetPreview() {
    VersionWidget(
        model = VersionItem(
            id = "a.version",
            padding = Padding(0)
        ),
        onAction = {}
    )
}
