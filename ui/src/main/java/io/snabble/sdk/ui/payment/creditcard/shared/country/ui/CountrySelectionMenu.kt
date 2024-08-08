package io.snabble.sdk.ui.payment.creditcard.shared.country.ui

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.payment.creditcard.shared.country.displayName
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.CountryItem
import io.snabble.sdk.ui.payment.creditcard.shared.country.domain.models.StateItem
import java.util.Locale

@Composable
internal fun CountrySelectionMenu(
    modifier: Modifier = Modifier,
    countryItems: List<CountryItem>?,
    selectedCountryCode: String? = null,
    selectedStateCode: String? = null,
    onCountrySelected: (CountryItem, StateItem?) -> Unit,
) {
    var showCountryList by remember { mutableStateOf(false) }
    var dismissCountryList by remember { mutableStateOf(true) }

    var showStateList by remember { mutableStateOf(false) }
    var dismissStateList by remember { mutableStateOf(true) }

    var currentCountryItem by remember {
        mutableStateOf(
            selectedCountryCode?.let { countryCode -> countryItems?.firstOrNull { it.code == countryCode } }
                ?: countryItems.loadDefaultCountry()
                    .also { country -> onCountrySelected(country, null) }
        )
    }

    var currentStateItem by remember {
        mutableStateOf(
            selectedStateCode?.let { stateCode ->
                currentCountryItem.stateItems?.firstOrNull { it.code == stateCode }
            }
        )
    }

    fun validateStateItem() {
        if (currentCountryItem.stateItems?.contains(currentStateItem) != true) {
            currentStateItem = null
        }
    }

    DropDownMenu(
        modifier = modifier,
        isExpanded = showCountryList && !dismissCountryList,
        onExpand = {
            showCountryList = !showCountryList
            dismissCountryList = false
        },
        onDismiss = { dismissCountryList = true },
        label = stringResource(id = R.string.Snabble_Payment_CustomerInfo_country),
        value = currentCountryItem.displayName,
        menuItems = countryItems
    ) { country ->
        DropdownMenuItem(
            text = {
                Text(
                    text = country.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 17.sp
                )
            },
            onClick = {
                currentCountryItem = country
                validateStateItem()
                onCountrySelected(country, currentStateItem)
                showCountryList = false
            }
        )
    }
    if (currentCountryItem.stateItems != null) {
        DropDownMenu(
            modifier = modifier,
            isExpanded = showStateList && !dismissStateList,
            onExpand = {
                showStateList = !showStateList
                dismissStateList = false
            },
            onDismiss = { dismissStateList = true },
            label = stringResource(id = R.string.Snabble_Payment_CustomerInfo_state),
            value = currentStateItem?.displayName
                ?: stringResource(id = R.string.Snabble_Payment_CustomerInfo_stateSelect),
            menuItems = currentCountryItem.stateItems
        ) { state ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = state.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 17.sp
                    )
                },
                onClick = {
                    currentStateItem = state
                    onCountrySelected(currentCountryItem, state)
                    showStateList = false
                }
            )
        }
    }
}

private fun List<CountryItem>?.loadDefaultCountry(): CountryItem =
    this?.firstOrNull { it.displayName == Locale.getDefault().country.displayName }
        ?: this?.firstOrNull { it.code == Locale.GERMANY.displayCountry }
        ?: CountryItem(
            displayName = Locale.GERMANY.country.displayName,
            code = Locale.GERMANY.country,
            stateItems = null
        )
