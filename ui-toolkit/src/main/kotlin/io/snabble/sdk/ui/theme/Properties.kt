package io.snabble.sdk.ui.theme

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.snabble.sdk.home.resolveDimenId

data class Spacing(
    var default: Dp = 0.dp,
    var small: Dp = 4.dp,
    var medium: Dp = 8.dp,
    var large: Dp = 16.dp
)

var LocalSpacing = compositionLocalOf {
    Spacing()
}

@Composable
fun Spacing.mergeSpacing(): Spacing {

    val default = LocalContext.current.resolveDimenId("default")
    val small = LocalContext.current.resolveDimenId("small")
    val medium = LocalContext.current.resolveDimenId("medium")
    val large = LocalContext.current.resolveDimenId("large")

    if (default != null) {
        this.default = dimensionResource(id = default)
    }
    if (small != null) {
        this.small = dimensionResource(id = small)
        Log.d("TAG", "mergeSpacing: ${dimensionResource(id = small)}")
    }
    if (medium != null) {
        this.medium = dimensionResource(id = medium)
    }
    if (large != null) {
        this.large = dimensionResource(id = large)
    }
    return this
}

val MaterialTheme.spacing: Spacing
    @Composable
    get() = LocalSpacing.current