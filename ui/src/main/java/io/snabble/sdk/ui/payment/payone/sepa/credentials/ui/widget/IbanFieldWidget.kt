package io.snabble.sdk.ui.payment.payone.sepa.credentials.ui.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun IbanFieldWidget(
    iban: String,
    onIbanChange: (String) -> Unit,
    onAction: () -> Unit,
) {
    @OptIn(ExperimentalMaterial3Api::class)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier
                .wrapContentSize()
                .weight(1f),
            value = "DE",
            onValueChange = {},
            label = {},
            readOnly = true,
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = iban,
            onValueChange = onIbanChange,
            modifier = Modifier
                .padding(start = 8.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            label = { Text(text = "IBAN") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = { onAction() }
            ),
            maxLines = 1,
            singleLine = true,
        )
    }
}
