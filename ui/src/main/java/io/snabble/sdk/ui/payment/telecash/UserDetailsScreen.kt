package io.snabble.sdk.ui.payment.telecash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.snabble.sdk.ui.cart.shoppingcart.utils.rememberTextFieldManager
import io.snabble.sdk.ui.payment.telecash.domain.Address
import io.snabble.sdk.ui.payment.telecash.domain.UserDetails
import io.snabble.sdk.ui.payment.telecash.widget.TextInput

@Composable
fun UserDetailsScreen(
    onSendAction: (UserDetails) -> Unit,
    onErrorProcessed: () -> Unit,
    showError: Boolean,
    isLoading: Boolean,
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

    val createUserDetails: () -> UserDetails = {
        UserDetails(
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
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextInput(
            value = name,
            onValueChanged = {
                name = it
            },
            label = "Name",
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            )
        )
        TextInput(
            value = phoneNumber,
            onValueChanged = {
                phoneNumber = it
            },
            label = "Handynummer",
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            )
        )
        TextInput(
            value = email,
            onValueChanged = {
                email = it
            },
            label = "Email",
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            )
        )
        TextInput(
            value = street,
            onValueChanged = {
                street = it
            },
            label = "Straße",
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            )
        )
        TextInput(
            value = zip,
            onValueChanged = {
                zip = it
            },
            label = "Postleitzahl",
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            )
        )
        TextInput(
            value = city,
            onValueChanged = {
                city = it
            },
            label = "Stadt",
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            )
        )
        TextInput(
            value = state,
            onValueChanged = {
                state = it
            },
            label = "Bundesland",
            keyboardActions = KeyboardActions(
                onNext = { textFieldManager.moveFocusToNext() }
            )
        )
        TextInput(
            value = country,
            onValueChanged = {
                country = it
            },
            label = "Country",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onNext = {
                    textFieldManager.clearFocusAndHideKeyboard()
                    onSendAction(createUserDetails())
                }
            )
        )
        Button(onClick = { onSendAction(createUserDetails()) }) {
            Text("Senden")
        }
    }
}
