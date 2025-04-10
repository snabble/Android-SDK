package io.snabble.sdk.ui.payment.creditcard.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.ThemeManager
import io.snabble.sdk.ui.cart.shoppingcart.utils.rememberTextFieldManager
import io.snabble.sdk.ui.payment.creditcard.shared.country.displayName
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.Address
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CountryItem
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CustomerInfo
import io.snabble.sdk.ui.payment.creditcard.shared.country.ui.CountrySelectionMenu
import io.snabble.sdk.ui.payment.creditcard.shared.widget.PhoneNumberInput
import io.snabble.sdk.ui.payment.creditcard.shared.widget.TextInput
import java.util.Locale

@Composable
internal fun CustomerInfoInputScreen(
    onSendAction: (CustomerInfo) -> Unit,
    onErrorProcessed: () -> Unit,
    showError: Boolean,
    isLoading: Boolean,
    countryItems: List<CountryItem>,
    onBackNavigationClick: () -> Unit,
) {
    val preFilledData = Snabble.formPrefillData
    val primaryButtonConfig = ThemeManager.primaryButtonConfig
    val secondaryButtonConfig = ThemeManager.secondaryButtonConfig

    var name by remember { mutableStateOf(preFilledData?.name.orEmpty()) }
    var intCallingCode by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(preFilledData?.email.orEmpty()) }
    var street by remember { mutableStateOf(preFilledData?.street.orEmpty()) }
    var zip by remember { mutableStateOf(preFilledData?.zip.orEmpty()) }
    var city by remember { mutableStateOf(preFilledData?.city.orEmpty()) }
    var state by remember { mutableStateOf(preFilledData?.stateCode.orEmpty()) }

    var country: CountryItem by remember {
        mutableStateOf(
            countryItems.loadPreSetCountry(preFilledData?.countryCode)
                ?: countryItems.loadDefaultCountry()
        )
    }

    val textFieldManager = rememberTextFieldManager()

    val isRequiredStateSet =
        if (!countryItems.firstOrNull { it.code == country.code }?.stateItems.isNullOrEmpty()) {
            state.isNotEmpty()
        } else {
            true
        }

    val areRequiredFieldsSet = listOf(
        name,
        intCallingCode,
        phoneNumber,
        email,
        street,
        zip,
        city,
        country.code
    ).all { it.isNotEmpty() } && isRequiredStateSet

    val createCustomerInfo: () -> CustomerInfo = {
        CustomerInfo(
            name = name,
            intCallingCode = intCallingCode,
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
        PhoneNumberInput(
            callingCode = intCallingCode,
            onCallingCodeChanged = { callingCode -> intCallingCode = callingCode },
            phoneNumber = phoneNumber,
            onPhoneNumberChanged = { number -> phoneNumber = number },
            onKeyboardAction = { textFieldManager.moveFocusToNext() },
            onErrorProcessed = onErrorProcessed,
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
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
            selectedStateCode = state,
            onCountrySelected = { countryItem, stateItem ->
                country = countryItem
                state = stateItem?.code.orEmpty()
                if (showError) onErrorProcessed()
            }
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = primaryButtonConfig.minHeight.dp),
                onClick = { onSendAction(createCustomerInfo()) },
                enabled = !isLoading && areRequiredFieldsSet
            ) {
                Text(
                    stringResource(R.string.Snabble_Payment_CustomerInfo_next),
                    fontSize = primaryButtonConfig.textSize.sp
                )
            }
            AnimatedVisibility(visible = showError) {
                Text(
                    stringResource(R.string.Snabble_Payment_CustomerInfo_error),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (!secondaryButtonConfig.useOutlinedButton) {
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBackNavigationClick
            ) {
                Text(text = stringResource(R.string.Snabble_cancel))
            }
        } else {
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = secondaryButtonConfig.minHeight.dp),
                onClick = onBackNavigationClick,
                border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = stringResource(R.string.Snabble_cancel),
                    fontSize = secondaryButtonConfig.textSize.sp
                )
            }
        }
    }
}

private fun List<CountryItem>?.loadPreSetCountry(countryCode: String?): CountryItem? =
    this?.firstOrNull { it.code == countryCode }

private fun List<CountryItem>?.loadDefaultCountry(): CountryItem =
    this?.firstOrNull { it.displayName == Locale.getDefault().country.displayName }
        ?: this?.firstOrNull { it.code == Locale.GERMANY.displayCountry }
        ?: CountryItem(
            displayName = Locale.GERMANY.country.displayName,
            code = Locale.GERMANY.country,
            numericCode = "276",
            stateItems = null
        )
