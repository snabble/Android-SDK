package io.snabble.sdk.ui.payment.payone.sepa.ui.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun TextFieldWidget(
    name: String,
    label: String,
    readOnly: Boolean,
    onStringChange: (String) -> Unit,
    onAction: () -> Unit,
) {
    @OptIn(ExperimentalMaterial3Api::class)
    OutlinedTextField(
        value = name,
        onValueChange = onStringChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { Text(text = label) },
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
            onSend = { onAction() }
        ),
        maxLines = 1,
        singleLine = true,
    )
}
