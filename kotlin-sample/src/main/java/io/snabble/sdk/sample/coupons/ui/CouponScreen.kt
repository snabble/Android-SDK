package io.snabble.sdk.sample.coupons.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.snabble.sdk.sample.coupons.ui.widget.CouponWidget

@Composable
fun CouponScreen(couponViewModel: CouponViewModel) {
    val coupons = couponViewModel.coupons.collectAsStateWithLifecycle().value

    if (coupons.isNotEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .height(intrinsicSize = IntrinsicSize.Max)
                    .background(color = Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                coupons.forEach { couponItem ->
                    CouponWidget(couponItem = couponItem, onclick = { couponViewModel.onEvent(ShowCoupon(it)) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
