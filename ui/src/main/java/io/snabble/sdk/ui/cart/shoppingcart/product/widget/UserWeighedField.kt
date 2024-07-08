package io.snabble.sdk.ui.cart.shoppingcart.product.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.shoppingcart.utils.rememberTextFieldManager

@Composable
internal fun UserWeighedField(
    weight: String,
    quantityAnnotation: String,
    onQuantityChanged: (Int) -> Unit,
    onDeleteWeighed: () -> Unit
) {
    var value by remember { mutableStateOf(TextFieldValue(weight)) }
    var showApplyButton by remember { mutableStateOf(false) }

    val textFieldManager = rememberTextFieldManager()

    if (weight.toInt() > 0) {
        Row(
            modifier = Modifier.widthIn(min = 88.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedIconButton(
                modifier = Modifier.size(36.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f)
                ),
                onClick = { onDeleteWeighed() }) {
                Icon(
                    painter = painterResource(id = R.drawable.snabble_ic_delete),
                    contentDescription = stringResource(
                        id = R.string.Snabble_Shoppingcart_Accessibility_actionDelete
                    ),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            OutlinedTextField(
                modifier = Modifier.width(80.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f),
                ),
                placeholder = {
                    Text(text = stringResource(id = R.string.Snabble_Shoppingcart_Accessibility_quantity))
                },
                value = value,
                onValueChange = {
                    if (it.text.isNotBlank() &&
                        it.text.toInt() > 0 &&
                        it.text.toInt() < ShoppingCart.MAX_QUANTITY
                    ) {
                        if (it.text != value.text) showApplyButton = true
                        value = it
                    }
                },
                keyboardActions = KeyboardActions(onDone = {
                    onQuantityChanged(value.text.toInt())
                    showApplyButton = false
                    textFieldManager.clearFocusAndHideKeyboard()
                }),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = quantityAnnotation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (showApplyButton) {
                OutlinedIconButton(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f)
                    ),
                    onClick = {
                        onQuantityChanged(value.text.toInt())
                        showApplyButton = false
                        textFieldManager.clearFocusAndHideKeyboard()
                    }) {
                    Icon(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(6.dp),
                        painter = painterResource(id = R.drawable.snabble_ic_check_white),
                        contentDescription = stringResource(
                            id = R.string.Snabble_Shoppingcart_Accessibility_actionDelete
                        ),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
