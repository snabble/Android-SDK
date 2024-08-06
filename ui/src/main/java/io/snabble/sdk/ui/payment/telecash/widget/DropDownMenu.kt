package io.snabble.sdk.ui.payment.telecash.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties

@Composable
fun <T> DropDownMenu(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    label: String,
    value: String,
    menuItems: List<T>?,
    content: @Composable (T) -> Unit,
) {
    var maxWidth by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpand() }
                    .onGloballyPositioned {
                        maxWidth = it.size
                    },
                value = value,
                onValueChange = {},
                textStyle = MaterialTheme.typography.bodyLarge,
                label = {
                    Text(
                        text = label,
                        fontSize = 17.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    disabledContainerColor = MaterialTheme.colorScheme.background,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface
                ),
                trailingIcon = {
                    Icon(
                        if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = ""
                    )
                },
                readOnly = true,
                enabled = false
            )
        }
        DropdownMenu(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .height(300.dp)
                .widthIn(min = (maxWidth.width / density.density).dp),
            expanded = isExpanded,
            onDismissRequest = onDismiss,
            properties = PopupProperties(
                clippingEnabled = false,
                dismissOnClickOutside = true
            )
        ) {
            menuItems?.forEach { selectedItem ->
                content(selectedItem)
                HorizontalDivider(
                    thickness = Dp.Hairline,
                    color = Color.LightGray
                )
            }
        }
    }
}
