package io.snabble.sdk.ui.cart.shoppingcart.product.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ExtraImage(hasCoupon: Boolean, isAgeRestricted: Boolean, age: Long, isManualApplied: Boolean) {
    if (hasCoupon || (isAgeRestricted && age > 0)) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .size(20.dp)
                .background(if (!isManualApplied) Color(0xFF999999) else Color.Red),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.background(Color.Red),
                text = if (hasCoupon) "%" else age.toString(),
                color = Color.White
            )
        }
    }
}
