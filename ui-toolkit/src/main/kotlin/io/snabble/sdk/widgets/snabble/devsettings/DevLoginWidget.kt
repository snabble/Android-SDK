package io.snabble.sdk.widgets.snabble.devsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType.Companion.Password
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun Preview() {
    DevLoginWidget(
        dismiss = {},
        login = {},
        showError = true,
        onPasswordChange = {}
    )
}

@Composable
fun DevLoginWidget(
    dismiss: () -> Unit,
    login: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    showError: Boolean,
) {
    val focusManager = LocalFocusManager.current

    var password by rememberSaveable { mutableStateOf("") }
    Card(modifier = Modifier
        .wrapContentSize()) {
        Column(modifier = Modifier
            .wrapContentSize()) {
            Spacer(Modifier.height(16.dp))
            PasswordField(
                password = password,
                onPasswordChange = {
                    onPasswordChange(it)
                    password = it
                },
                focusManager = focusManager)
            if (showError) {
                Spacer(modifier = Modifier.heightIn(8.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = "Wrong password!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    modifier = Modifier.widthIn(min = 115.dp),
                    onClick = { login(password) }) {
                    Text(text = "Activate")
                }
                Button(
                    modifier = Modifier.widthIn(min = 115.dp),
                    onClick = {
                        dismiss()
                    }) {
                    Text(text = "Cancel")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    focusManager: FocusManager,
) {
    @OptIn(ExperimentalMaterial3Api::class)
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        label = { androidx.compose.material3.Text(text = "Enter password") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = Password,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Next)
            }
        ),
        maxLines = 1,
        singleLine = true,
    )
}
