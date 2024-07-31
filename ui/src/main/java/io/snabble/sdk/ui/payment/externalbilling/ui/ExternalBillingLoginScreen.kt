package io.snabble.sdk.ui.payment.externalbilling.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.payment.externalbilling.ui.widgets.PasswordField
import io.snabble.sdk.ui.payment.payone.sepa.form.ui.widget.TextFieldWidget

@Composable
internal fun ExternalBillingLoginScreen(
    onSaveClick: (username: String, password: String) -> Unit,
    onFocusChanged: () -> Unit,
    isInputValid: Boolean,
    errorMessage: String = stringResource(id = R.string.Snabble_Payment_ExternalBilling_Error_wrongCredentials)
) {
    val focusManager = LocalFocusManager.current

    val username = rememberSaveable() {
        mutableStateOf("")
    }
    val password = rememberSaveable() {
        mutableStateOf("")
    }
    val passwordVisible = rememberSaveable { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
            .padding(all = 16.dp),
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(id = R.string.Snabble_Payment_ExternalBilling_message),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextFieldWidget(
            value = username.value,
            label = stringResource(id = R.string.Snabble_Payment_ExternalBilling_username),
            onValueChange = {
                username.value = it
                onFocusChanged()
            },
            readOnly = false,
            focusManager = focusManager
        )
        Spacer(modifier = Modifier.height(8.dp))
        PasswordField(
            value = password.value,
            onValueChanged = {
                password.value = it
                onFocusChanged()
            },
            label = stringResource(id = R.string.Snabble_Payment_ExternalBilling_password),
            passwordVisible = passwordVisible.value,
            focusManager = focusManager,
            onSaveClick = { onSaveClick(username.value, password.value) },
            onVisibilityClick = { passwordVisible.value = !passwordVisible.value })
        if (!isInputValid) {
            Spacer(modifier = Modifier.heightIn(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
            enabled = password.value.isNotEmpty() && username.value.isNotEmpty(),
            shape = MaterialTheme.shapes.extraLarge,
            onClick = {
                onSaveClick(username.value, password.value)
                keyboardController?.hide()
            },
        ) {
            Text(text = stringResource(id = R.string.Snabble_Payment_ExternalBilling_add))
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun ExternalBillingLoginScreenPreview() {
    ExternalBillingLoginScreen(
        onSaveClick = { _, _ -> },
        isInputValid = true,
        onFocusChanged = {}
    )
}
