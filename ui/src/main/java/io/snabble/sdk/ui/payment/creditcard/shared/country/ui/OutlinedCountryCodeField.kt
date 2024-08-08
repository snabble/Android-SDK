package io.snabble.sdk.ui.payment.creditcard.shared.country.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import io.snabble.sdk.ui.payment.creditcard.shared.widget.defaultColors

@Composable
internal fun OutlinedCountryCodeField(
    modifier: Modifier = Modifier,
    label: String,
    countryCode: String,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(key1 = enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)?.let {
                        onClick()
                    }
                }
            },
        readOnly = true,
        value = countryCode,
        onValueChange = { },
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(text = label) },
        maxLines = 1,
        singleLine = true,
        enabled = enabled,
        isError = isError,
        colors = TextFieldDefaults.defaultColors()
    )
}
