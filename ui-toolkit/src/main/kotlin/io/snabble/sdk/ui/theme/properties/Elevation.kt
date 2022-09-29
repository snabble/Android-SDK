package io.snabble.sdk.ui.theme.properties

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.snabble.sdk.ui.toolkit.R

data class Elevation(
    val default: Dp = 0.dp,
    val small: Dp = 0.dp,
    val medium: Dp = 0.dp,
    val large: Dp = 0.dp,
)

val LocalElevation = compositionLocalOf { Elevation() }

@Composable
fun Elevation.applyElevation(): Elevation = Elevation(
    default = dimensionResource(id = R.dimen.default_elevation),
    small = dimensionResource(id = R.dimen.small_elevation),
    medium = dimensionResource(id = R.dimen.medium_elevation),
    large = dimensionResource(id = R.dimen.large_elevation),
)

val MaterialTheme.elevation: Elevation
    @Composable
    @ReadOnlyComposable
    get() = LocalElevation.current