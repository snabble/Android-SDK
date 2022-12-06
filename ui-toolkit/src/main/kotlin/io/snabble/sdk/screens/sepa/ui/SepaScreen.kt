package io.snabble.sdk.screens.sepa.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import io.snabble.sdk.screens.sepa.data.PayOneSepaData
import io.snabble.sdk.screens.sepa.ui.widget.IbanFieldWidget
import io.snabble.sdk.screens.sepa.ui.widget.TextFieldWidget

@Composable
fun PayOneSepaScreen(
    saveData: (data: PayOneSepaData) -> Unit,
    validateText: (String) -> Unit,
    validateIban: (String) -> Unit,
) {

    var lastName by rememberSaveable { mutableStateOf("") }
    var iban by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        TextFieldWidget(
            name = lastName,
            label = "Nachname",
            onStringChange = {
                validateText(it)
                lastName = it
            },
            readOnly = false,
            onAction = {}
        )
        IbanFieldWidget(
            iban = iban,
            onIbanChange = {
                validateIban(it)
                iban = it
            },
            onAction = {}
        )
        TextFieldWidget(
            name = city,
            label = "Stadt",
            readOnly = false,
            onStringChange = {
                validateText(it)
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
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            shape = MaterialTheme.shapes.extraLarge,
            onClick = {
                saveData(
                    PayOneSepaData(
                        name = lastName,
                        iban = "DE" + iban,
                        city = city,
                        country = "Deutschlande",
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
    PayOneSepaScreen(
        saveData = {},
        validateText = {},
        validateIban = {}
    )
}
