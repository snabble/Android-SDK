package io.snabble.sdk.ui.cart.shoppingcart.depositreturn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.shoppingcart.data.item.DepositReturnVoucher
import io.snabble.sdk.shoppingcart.data.item.ItemType
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.shoppingcart.product.model.DepositReturnItem

@Composable
fun DepositReturn(
    modifier: Modifier = Modifier,
    item: DepositReturnItem,
    showHint: Boolean = false,
    onDeleteClick: (ShoppingCart.Item) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 16.dp)
            .heightIn(min = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.Snabble_ShoppingCart_DepositReturn_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.totalDeposit,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            OutlinedIconButton(
                modifier = Modifier.size(36.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.33f)
                ),
                onClick = { onDeleteClick(item.item) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.snabble_ic_delete),
                    contentDescription = stringResource(
                        id = R.string.Snabble_Shoppingcart_Accessibility_actionDelete
                    ),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

        }
        if (showHint) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.Snabble_ShoppingCart_DepositReturn_message),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = Bold,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

    }
}

@PreviewLightDark
@Composable
private fun PreviewWithHint() {
    Surface {
        DepositReturn(
            item = DepositReturnItem(
                item = ShoppingCart.Item(
                    ShoppingCart(),
                    DepositReturnVoucher("", "", 1, ItemType.DEPOSIT_RETURN_VOUCHER)
                ),
                totalDeposit = "-0.75€"
            ),
            showHint = true,
            onDeleteClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewWithoutHint() {
    Surface {
        DepositReturn(
            item = DepositReturnItem(
                item = ShoppingCart.Item(
                    ShoppingCart(),
                    DepositReturnVoucher("", "", 1, ItemType.DEPOSIT_RETURN_VOUCHER)
                ),
                totalDeposit = "-0.75€"
            ),
            showHint = false,
            onDeleteClick = {}
        )
    }
}
