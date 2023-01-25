package io.snabble.sdk.ui.payment.payone.sepa.form.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.payment.payone.sepa.form.ui.widget.IbanFieldWidget
import io.snabble.sdk.ui.payment.payone.sepa.form.ui.widget.TextFieldWidget

@Composable
internal fun PayoneSepaFormScreen(
    name: String = "",
    onNameChange: (String) -> Unit,
    ibanNumber: String = "",
    onIbanNumberChange: (String) -> Unit,
    city: String = "",
    onCityChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    isIbanValid: Boolean,
    areAllInputsValid: Boolean,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(all = 16.dp),
    ) {
        Text(
            modifier = Modifier.align(CenterHorizontally),
            text = stringResource(id = R.string.Snabble_SEPA_helper),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextFieldWidget(
            value = name,
            label = stringResource(id = R.string.Snabble_Payment_SEPA_name),
            onValueChange = { onNameChange(it) },
            readOnly = false,
            focusManager = focusManager
        )
        Spacer(modifier = Modifier.height(8.dp))
        IbanFieldWidget(
            iban = ibanNumber,
            onIbanChange = { onIbanNumberChange(it) },
            focusManager = focusManager
        )

        Spacer(modifier = Modifier.height(8.dp))
        TextFieldWidget(
            value = city,
            label = stringResource(id = R.string.Snabble_Payment_SEPA_city),
            readOnly = false,
            onValueChange = { onCityChange(it) },
            onAction = {
                focusManager.clearFocus()
                if (areAllInputsValid) onSaveClick()
            },
            focusManager = focusManager,
            canSend = true,
        )

        Spacer(modifier = Modifier.height(8.dp))
        TextFieldWidget(
            value = "Deutschland",
            label = stringResource(id = R.string.Snabble_Payment_SEPA_countryCode),
            readOnly = true,
            enabled = false,
        )
        if (!isIbanValid && ibanNumber.isNotBlank()) {
            Spacer(modifier = Modifier.heightIn(8.dp))
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(id = R.string.Snabble_Payment_SEPA_invalidIBAN),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            enabled = areAllInputsValid,
            shape = MaterialTheme.shapes.extraLarge,
            onClick = { onSaveClick() },
        ) {
            Text(text = stringResource(id = R.string.Snabble_save))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.Snabble_Payment_SEPA_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f)
        )
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun PayoneSepaFormScreenPreview() {
    PayoneSepaFormScreen(
        onSaveClick = {},
        onIbanNumberChange = { it.isNotBlank() },
        onCityChange = { it.isNotBlank() },
        onNameChange = { it.isNotBlank() },
        isIbanValid = true,
        areAllInputsValid = true
    )
}
