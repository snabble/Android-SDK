package io.snabble.sdk.ui.payment.payone.sepa.form.ui.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction.Companion.Next
import androidx.compose.ui.text.input.KeyboardType.Companion.Number
import androidx.compose.ui.unit.dp
import io.snabble.sdk.ui.R

@Composable
fun IbanFieldWidget(
    iban: String,
    onIbanChange: (String) -> Unit,
    focusManager: FocusManager? = null,
) {
    @OptIn(ExperimentalMaterial3Api::class)
    Row(
        modifier = Modifier
            .wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = "DE",
            onValueChange = {},
            label = {},
            readOnly = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            enabled = false,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            singleLine = true,
            maxLines = 1,
        )
        OutlinedTextField(
            modifier = Modifier
                .weight(5f)
                .padding(start = 8.dp),
            value = iban,
            onValueChange = { input ->
                val clearedInput = input.replace('\n', ' ').replace('\r', ' ')
                onIbanChange(clearedInput)
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            label = { Text(text = stringResource(id = R.string.Snabble_Payment_SEPA_iban)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = Number,
                imeAction = Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager?.moveFocus(FocusDirection.Next) },
            ),
            maxLines = 1,
            singleLine = true,
        )
    }
}
