package io.snabble.sdk.ui.payment.creditcard.shared.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.snabble.countrycodepicker.ui.CountryCodePickerDialog
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.payment.creditcard.shared.country.ui.OutlinedCountryCodeField

@Composable
internal fun PhoneNumberInput(
    modifier: Modifier = Modifier,
    callingCode: String,
    onCallingCodeChanged: (String) -> Unit,
    enableCallingCodePicking: Boolean = true,
    phoneNumber: String,
    onPhoneNumberChanged: (String) -> Unit,
    enablePhoneNumberInput: Boolean = true,
    readOnlyPhoneNumberInput: Boolean = false,
    keyboardAction: ImeAction = ImeAction.Next,
    onKeyboardAction: () -> Unit,
    focusPhoneNumberInput: Boolean = false,
    errorMessage: String? = null,
    onErrorProcessed: () -> Unit,
) {
    var countryCode by rememberSaveable { mutableStateOf("") }
    var showCodePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val phoneNumberFocusRequester = remember { FocusRequester() }
            CountryCodePickerDialog(
                initialCallingCode = callingCode,
                showDialog = showCodePicker,
                // Called initially, so that the country code should never be empty
                onCountrySelected = { callingCode, flagEmoji ->
                    countryCode = "$flagEmoji $callingCode"
                    onCallingCodeChanged(callingCode)
                    showCodePicker = false
                },
                onDismissRequest = {
                    showCodePicker = false
                    if (phoneNumber.isBlank()) phoneNumberFocusRequester.requestFocus()
                }
            )

            val callingCodeFocusRequester = remember { FocusRequester() }
            if (showCodePicker) callingCodeFocusRequester.requestFocus()
            OutlinedCountryCodeField(
                modifier = Modifier
                    .widthIn(max = 120.dp)
                    .focusRequester(callingCodeFocusRequester)
                    .onFocusChanged { if (errorMessage != null) onErrorProcessed() },
                label = stringResource(id = R.string.Snabble_Payment_CustomerInfo_intCallingCode),
                countryCode = countryCode,
                onClick = { showCodePicker = true },
                enabled = enableCallingCodePicking,
                isError = errorMessage != null
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(phoneNumberFocusRequester)
                    .onFocusChanged {
                        if (errorMessage != null) onErrorProcessed()
                    },
                value = phoneNumber,
                onValueChange = { onPhoneNumberChanged(it) },
                textStyle = MaterialTheme.typography.bodyLarge,
                label = {
                    Text(
                        text = stringResource(id = R.string.Snabble_Payment_CustomerInfo_phoneNumber),
                        fontSize = 17.sp,
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = keyboardAction
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onKeyboardAction()
                    }
                ),
                isError = errorMessage != null,
                maxLines = 1,
                singleLine = true,
                enabled = enablePhoneNumberInput,
                readOnly = readOnlyPhoneNumberInput,
                colors = TextFieldDefaults.defaultColors()
            )
            if (focusPhoneNumberInput) phoneNumberFocusRequester.requestFocus()
        }
        if (errorMessage != null) {
            Text(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
