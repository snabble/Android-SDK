package io.snabble.sdk.widgets.snabble.devsettings.login.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction.Companion.Send
import androidx.compose.ui.text.input.KeyboardType.Companion.Password
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun DevSettingsLogin(
    dismiss: () -> Unit,
    login: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    showError: Boolean,
) {
    var password by rememberSaveable { mutableStateOf("") }
    Card(
        modifier = Modifier.wrapContentSize(),
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .wrapContentSize()
        ) {
            Spacer(Modifier.height(16.dp))
            PasswordField(
                password = password,
                onPasswordChange = {
                    onPasswordChange(it)
                    password = it
                },
                onAction = { login(password) }
            )
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    modifier = Modifier.widthIn(min = 90.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    onClick = { dismiss() },
                ) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(16.dp))
                TextButton(
                    modifier = Modifier.widthIn(min = 90.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    onClick = { login(password) },
                ) {
                    Text(
                        text = "Activate",
                        color = MaterialTheme.colorScheme.primary,
                    )
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
    onAction: () -> Unit,
) {
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
            imeAction = Send
        ),
        keyboardActions = KeyboardActions(
            onSend = { onAction() }
        ),
        maxLines = 1,
        singleLine = true,
    )
}

@Preview
@Composable
private fun Preview() {
    DevSettingsLogin(
        dismiss = {},
        login = {},
        showError = true,
        onPasswordChange = {}
    )
}
