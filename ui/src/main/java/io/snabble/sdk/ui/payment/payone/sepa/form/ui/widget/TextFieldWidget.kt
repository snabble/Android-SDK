package io.snabble.sdk.ui.payment.payone.sepa.form.ui.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
    onStringChange: (String) -> Unit = {},
    onAction: () -> Unit = {},
    focusManager: FocusManager? = null,
    enabled: Boolean = true,
    canSend: Boolean = false,
) {
    @OptIn(ExperimentalMaterial3Api::class)
    OutlinedTextField(
        value = value,
        onValueChange = onStringChange,
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
        colors = TextFieldDefaults.outlinedTextFieldColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
        )
    )
}
