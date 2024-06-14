package io.snabble.sdk.ui.cart.shoppingcart.adapter.widgets.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.shoppingcart.row.ProductRow

@Composable
fun QuantityField(
    row: ProductRow,
    onQuantityChanged: (Int) -> Unit
) {

    row.quantityText?.let {
        Box(modifier = Modifier.fillMaxHeight()) {
            Row(
                modifier = Modifier.height(38.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                OutlinedIconButton(
                    modifier = Modifier.size(36.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f)
                    ),
                    onClick = {
                        row.item?.let {
                            onQuantityChanged(it.getQuantityMethod() - 1)
                        }
                    }) {
                    Icon(
                        painter = painterResource(
                            id = when (row.item?.quantity) {
                                1 -> R.drawable.snabble_ic_delete
                                else -> R.drawable.snabble_ic_minus
                            }
                        ),
                        contentDescription = stringResource(
                            id = R.string.Snabble_Shoppingcart_Accessibility_decreaseQuantity
                        ),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    modifier = Modifier.widthIn(min = 36.dp),
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                OutlinedIconButton(
                    modifier = Modifier.size(36.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f)
                    ),
                    onClick = {
                        row.item?.let {
                            onQuantityChanged(it.getQuantityMethod() + 1)
                        }
                    }) {
                    Icon(
                        painter = painterResource(id = R.drawable.snabble_ic_add),
                        contentDescription = stringResource(
                            id = R.string.Snabble_Shoppingcart_Accessibility_increaseQuantity
                        ),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
