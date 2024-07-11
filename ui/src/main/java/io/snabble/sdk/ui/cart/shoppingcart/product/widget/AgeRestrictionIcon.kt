package io.snabble.sdk.ui.cart.shoppingcart.product.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun AgeRestrictionIcon(isAgeRestricted: Boolean, age: Int) {
    if ((isAgeRestricted && age > 0)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .size(20.dp)
                .background(Color.Red),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.background(Color.Red),
                text = age.toString(),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
