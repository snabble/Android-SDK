package io.snabble.sdk.ui.payment.payone.sepa.form.ui.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction.Companion.Done
import androidx.compose.ui.text.input.ImeAction.Companion.Next
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun TextFieldWidget(
    value: String,
    label: String,
    readOnly: Boolean,
    onValueChange: (String) -> Unit = {},
    onAction: () -> Unit = {},
    focusManager: FocusManager? = null,
    enabled: Boolean = true,
    canSend: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val clearedInput = input.replace('\n', ' ').replace('\r', ' ')
            onValueChange(clearedInput)
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(text = label) },
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = if (canSend) Done else Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusManager?.moveFocus(FocusDirection.Next) },
            onDone = { onAction() }
        ),
        maxLines = 1,
        singleLine = true,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
        )
    )
}
