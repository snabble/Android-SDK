package io.snabble.sdk.ui.cart.shoppingcart.product.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldDefaults.contentPaddingWithLabel
import androidx.compose.material3.TextFieldDefaults.contentPaddingWithoutLabel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.shoppingcart.utils.rememberTextFieldManager

@Composable
internal fun UserWeightedField(
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShoppingCartTextField(
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
                    val newValue = it.text.toIntOrNull() ?: 0
                    if (newValue > 0 && newValue < ShoppingCart.MAX_QUANTITY) {
                        if (newValue != value.text.toIntOrNull()) showApplyButton = true
                        value = it
                    }
                },
                keyboardActions = KeyboardActions(onDone = {
                    onQuantityChanged(value.text.toInt())
                    showApplyButton = false
                    textFieldManager.clearFocusAndHideKeyboard()
                }),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.bodyMedium,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            )
            Text(
                text = quantityAnnotation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            val iconResId = when (showApplyButton) {
                true -> R.drawable.snabble_ic_check_white
                else -> R.drawable.snabble_ic_delete
            }
            val backgroundColor = when (showApplyButton) {
                true -> MaterialTheme.colorScheme.primary
                else -> Color.Transparent
            }
            val iconTint = when (showApplyButton) {
                true -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurface
            }
            OutlinedIconButton(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f)
                ),
                onClick = {
                    if (showApplyButton) {

                        onQuantityChanged(value.text.toInt())
                        showApplyButton = false
                        textFieldManager.clearFocusAndHideKeyboard()
                    } else {
                        onDeleteWeighed()
                    }
                }) {
                Icon(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .padding(6.dp),
                    painter = painterResource(id = iconResId),
                    contentDescription = "",
                    tint = iconTint
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingCartTextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    contentPadding: PaddingValues? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        keyboardActions = keyboardActions,
        keyboardOptions = keyboardOptions,
        interactionSource = interactionSource
    ) { innerTextField ->
        TextFieldDefaults.DecorationBox(
            value = value.toString(),
            visualTransformation = VisualTransformation.None,
            innerTextField = innerTextField,
            singleLine = true,
            enabled = true,
            placeholder = placeholder,
            label = label,
            interactionSource = interactionSource,
            contentPadding = when {
                contentPadding != null -> contentPadding
                label == null -> contentPaddingWithoutLabel()
                else -> contentPaddingWithLabel()
            }
        ) {
            OutlinedTextFieldDefaults.Container(
                enabled = true,
                isError = false,
                interactionSource = interactionSource,
                colors = colors,
                shape = OutlinedTextFieldDefaults.shape
            )
        }
    }
}
