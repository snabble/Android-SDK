package io.snabble.sdk.ui.cart.shoppingcart.coupon

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.checkout.LineItem
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.cart.shoppingcart.product.model.CouponItem

@Composable
internal fun Coupon(couponItem: CouponItem, onDelete: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(if (couponItem.areRequirementsMet) 1f else 0.5f),
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                couponItem.name?.let {
                    Text(
                        it,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (!couponItem.areRequirementsMet) {
                    Text(
                        text = stringResource(id = R.string.Snabble_Coupon_notUsable),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Icon(
                Icons.Outlined.DeleteOutline,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(28.dp)
                    .clickable { if (couponItem.couponId != null) onDelete(couponItem.couponId) },
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    Coupon(
        couponItem = CouponItem(
            item = ShoppingCart.Item(ShoppingCart(), LineItem(id = "", amount = 1)),
            name = "TEST COUPON",
            areRequirementsMet = true,
            couponId = "123"
        )
    ) {}
}

@Preview
@Composable
private fun PreviewRequirementMissing() {
    Coupon(
        couponItem = CouponItem(
            item = ShoppingCart.Item(ShoppingCart(), LineItem(id = "", amount = 1)),
            name = "TEST COUPON",
            areRequirementsMet = false,
            couponId = "123"
        )
    ) {}
}
