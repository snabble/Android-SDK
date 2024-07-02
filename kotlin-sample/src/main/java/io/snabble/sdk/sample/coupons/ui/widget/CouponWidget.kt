package io.snabble.sdk.sample.coupons.ui.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.snabble.sdk.coupons.Coupon
import io.snabble.sdk.coupons.CouponType
import io.snabble.sdk.dynamicview.theme.ThemeWrapper
import io.snabble.sdk.ui.coupon.CouponItem

@Composable
fun CouponWidget(modifier: Modifier = Modifier, couponItem: CouponItem, onclick: (CouponItem) -> Unit) {
    val configuration = LocalConfiguration.current

    val expire = couponItem.buildExpireString(LocalContext.current.resources)

    couponItem.coupon?.let { item ->
        Surface(
            modifier = Modifier
                .width(multiplyScreenWidthWithScreenFactor(configuration.screenWidthDp).dp)
                .fillMaxHeight()
                .clickable { onclick(couponItem) }
                .then(modifier),
            shadowElevation = 4.dp,
            shape = CardDefaults.shape
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                CouponImageWidget(
                    modifier = Modifier
                        .height(160.dp),
                    url = item.image?.bestResolutionUrl
                )
                Spacer(modifier = Modifier.size(16.dp))
                CouponHeaderWidget(
                    name = item.name,
                    description = item.description.toString()
                )
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 16.dp)
                )
                CouponFooterWidget(
                    promotionDescription = item.promotionDescription,
                    expire = expire
                )
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
@Preview(showBackground = true, heightDp = 300)
private fun LightPreview() {
    ThemeWrapper {
        CouponWidget(
            couponItem = CouponItem(
                "",
                Coupon(
                    "1",
                    "Demo Coupon",
                    "Everything is free",
                    "Use it as often as u like",
                    CouponType.MANUAL,
                    null,
                    null,
                    "2021-04-25T22:00:00Z",
                    "2026-07-25T22:00:00Z",
                    null,
                    null,
                    null
                )
            ),
            onclick = {}
        )
    }
}

private fun multiplyScreenWidthWithScreenFactor(width: Int): Float = when (width) {
    in ZERO_SCREEN_WIDTH..MEDIUM_SCREEN_WIDTH -> width * MEDIUM_SCALE_FACTOR
    in MEDIUM_SCREEN_WIDTH..LARGE_SCREEN_WIDTH -> width * LARGE_SCALE_FACTOR
    else -> width * XL_SCALE_FACTOR
}

private const val ZERO_SCREEN_WIDTH = 0
private const val MEDIUM_SCREEN_WIDTH = 360
private const val LARGE_SCREEN_WIDTH = 480

private const val MEDIUM_SCALE_FACTOR = .7f
private const val LARGE_SCALE_FACTOR = .6f
private const val XL_SCALE_FACTOR = .5f
