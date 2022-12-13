package io.snabble.sdk.ui.payment.payone.sepa.credentials.ui

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.payment.payone.sepa.PayoneSepaData
import io.snabble.sdk.ui.payment.payone.sepa.credentials.ui.widget.IbanFieldWidget
import io.snabble.sdk.ui.payment.payone.sepa.credentials.ui.widget.TextFieldWidget

@Composable
fun PayoneSepaScreen(
    saveData: (data: PayoneSepaData) -> Unit,
    validateIban: (String) -> Unit,
    isIbanValid: Boolean,
) {
    var lastName by rememberSaveable { mutableStateOf("") }
    var iban by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }

    val enableButton = lastName.isNotBlank() && city.isNotBlank() && isIbanValid
    Log.d("foo", "isValid? $isIbanValid")

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        TextFieldWidget(
            name = lastName,
            label = "Nachname",
            onStringChange = {
                lastName = it
            },
            readOnly = false,
            onAction = {}
        )
        IbanFieldWidget(
            iban = iban,
            onIbanChange = {
                validateIban("DE$it")
                iban = it
            },
            onAction = {}
        )
        TextFieldWidget(
            name = city,
            label = "Stadt",
            readOnly = false,
            onStringChange = {
                city = it
            },
            onAction = {}
        )
        TextFieldWidget(
            name = "Deutschland",
            label = "Land",
            readOnly = true,
            onStringChange = {},
            onAction = {}
        )
        if (!isIbanValid && iban.isNotBlank()) {
            Spacer(modifier = Modifier.heightIn(8.dp))
            androidx.compose.material.Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = "Invalid IBAN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            enabled = enableButton,
            shape = MaterialTheme.shapes.extraLarge,
            onClick = {
                saveData(
                    PayoneSepaData(
                        name = lastName,
                        iban = "DE$iban",
                        city = city,
                        countryCode = "DE",
                    )
                )
            },
        ) {
            Text(text = "Speichern")
        }
    }
}

@Preview
@Composable
fun Preview() {
    PayoneSepaScreen(
        saveData = {},
        validateIban = { it.isNotBlank() },
        isIbanValid = true
    )
}
