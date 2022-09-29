package io.snabble.sdk.ui.theme.properties

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.snabble.sdk.ui.toolkit.R

data class Padding(
    val default: Dp = 0.dp,
    val small: Dp = 0.dp,
    val medium: Dp = 0.dp,
    val large: Dp = 0.dp,
)

val LocalPadding = compositionLocalOf { Padding() }

@Composable
fun Padding.applyPadding(): Padding = Padding(
    default = dimensionResource(id = R.dimen.default_padding),
    small = dimensionResource(id = R.dimen.small_padding),
    medium = dimensionResource(id = R.dimen.medium_padding),
    large = dimensionResource(id = R.dimen.large_padding),
)

val MaterialTheme.padding: Padding
    @Composable
    @ReadOnlyComposable
    get() = LocalPadding.current