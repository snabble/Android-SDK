package io.snabble.sdk.ui.payment.telecash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.snabble.sdk.BuildConfig
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.shoppingcart.utils.rememberTextFieldManager
import io.snabble.sdk.ui.payment.telecash.domain.Address
import io.snabble.sdk.ui.payment.telecash.domain.CustomerInfo
import io.snabble.sdk.ui.payment.telecash.widget.TextInput

@Composable
fun CustomerInfoInputScreen(
    onSendAction: (CustomerInfo) -> Unit,
    onErrorProcessed: () -> Unit,
    showError: Boolean,
    isLoading: Boolean,
    onBackNavigationClick: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var zip by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    if (BuildConfig.DEBUG) {
        name = "Max Mustermann"
        phoneNumber = "+491729973186"
        email = "max.mustermann@example.com123"
        street = "Fakestr. 123"
        zip = "12345"
        city = "Bonn"
        state = "NRW"
        country = "DE"
    }

    val textFieldManager = rememberTextFieldManager()

    val createCustomerInfo: () -> CustomerInfo = {
        CustomerInfo(
            name = name,
            phoneNumber = phoneNumber,
            email = email,
            address = Address(
                street = street, zip = zip, city = city, state = state, country = country
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = name,
            onValueChanged = { name = it },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_fullName),
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            )
        )
        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = phoneNumber,
            onValueChanged = { phoneNumber = it },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_phoneNumber),
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            )
        )
        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = email,
            onValueChanged = { email = it },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_email),
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            ),
        )
        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = street,
            onValueChanged = { street = it },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_street),
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            ),
        )
        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = zip,
            onValueChanged = { zip = it },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_zip),
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            ),
        )
        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = city,
            onValueChanged = { city = it },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_city),
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            ),
        )
        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = state,
            onValueChanged = { state = it },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_state),
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            ),
        )
        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = country,
            onValueChanged = { country = it },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_country),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send,
                capitalization = KeyboardCapitalization.Words
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    textFieldManager.clearFocusAndHideKeyboard()
                    onSendAction(createCustomerInfo())
                }
            )
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSendAction(createCustomerInfo()) }
        ) {
            Text(stringResource(R.string.Snabble_Payment_CustomerInfo_next))
        }
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onBackNavigationClick
        ) {
            Text(text = stringResource(R.string.Snabble_cancel))
        }
    }
}
