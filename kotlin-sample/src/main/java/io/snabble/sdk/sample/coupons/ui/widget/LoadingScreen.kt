package io.snabble.sdk.sample.coupons.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.snabble.sdk.dynamicview.theme.ThemeWrapper

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    circularIndicatorSize: Dp = 50.dp,
    circularIndicatorStrokeWidth: Dp = 5.dp,
    type: ProgressIndicatorType = Circular,
) {
    ThemeWrapper {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
            when (type) {
                Circular -> CircularProgressIndicator(
                    modifier = Modifier
                        .size(circularIndicatorSize),
                    color = MaterialTheme.colorScheme.primary,
                    strokeCap = StrokeCap.Round,
                    strokeWidth = circularIndicatorStrokeWidth
                )

                Linear -> LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )

                is CircularWithText -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(circularIndicatorSize),
                            color = MaterialTheme.colorScheme.primary,
                            strokeCap = StrokeCap.Round,
                            strokeWidth = circularIndicatorStrokeWidth
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = type.value,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

sealed interface ProgressIndicatorType
data object Circular : ProgressIndicatorType
data object Linear : ProgressIndicatorType
data class CircularWithText(val value: String) : ProgressIndicatorType
