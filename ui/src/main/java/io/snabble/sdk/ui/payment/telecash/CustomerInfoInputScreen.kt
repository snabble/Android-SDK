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
import androidx.compose.material3.MaterialTheme
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
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.shoppingcart.utils.rememberTextFieldManager
import io.snabble.sdk.ui.payment.telecash.domain.Address
import io.snabble.sdk.ui.payment.telecash.domain.CustomerInfo
import io.snabble.sdk.ui.payment.telecash.domain.model.country.CountryItem
import io.snabble.sdk.ui.payment.telecash.widget.CountrySelectionMenu
import io.snabble.sdk.ui.payment.telecash.widget.TextInput

@Composable
fun CustomerInfoInputScreen(
    onSendAction: (CustomerInfo) -> Unit,
    onErrorProcessed: () -> Unit,
    showError: Boolean,
    isLoading: Boolean,
    countryItems: List<CountryItem>?,
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
            onValueChanged = {
                name = it
                if (showError) onErrorProcessed()
            },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_fullName),
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            )
        )
        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = phoneNumber,
            onValueChanged = {
                phoneNumber = it
                if (showError) onErrorProcessed()
            },
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
            onValueChanged = {
                email = it
                if (showError) onErrorProcessed()
            },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_email),
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            ),
        )
        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = street,
            onValueChanged = {
                street = it
                if (showError) onErrorProcessed()
            },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_street),
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            ),
        )
        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = zip,
            onValueChanged = {
                zip = it
                if (showError) onErrorProcessed()
            },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_zip),
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            ),
        )
        TextInput(
            modifier = Modifier.fillMaxWidth(),
            value = city,
            onValueChanged = {
                city = it
                if (showError) onErrorProcessed()
            },
            label = stringResource(R.string.Snabble_Payment_CustomerInfo_city),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Words
            ),
            keyboardActions = KeyboardActions(
                onDone = { textFieldManager.clearFocusAndHideKeyboard() }
            )
        )
        CountrySelectionMenu(
            countryItems = countryItems,
            selectedCountryCode = country,
            selectedStateCode = null,
            onCountrySelected = { (_, countryCode), stateItem ->
                country = countryCode
                state = stateItem?.code.orEmpty()
                if (showError) onErrorProcessed()
            }
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSendAction(createCustomerInfo()) },
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.Snabble_Payment_CustomerInfo_next))
            }
            if (showError) {
                Text(
                    stringResource(R.string.Snabble_Payment_CustomerInfo_error),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onBackNavigationClick
        ) {
            Text(text = stringResource(R.string.Snabble_cancel))
        }
    }
}
