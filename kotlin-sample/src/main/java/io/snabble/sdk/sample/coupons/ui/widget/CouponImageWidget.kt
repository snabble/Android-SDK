package io.snabble.sdk.sample.coupons.ui.widget

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideSubcomposition
import com.bumptech.glide.integration.compose.RequestState
import com.bumptech.glide.load.engine.DiskCacheStrategy
import io.snabble.sdk.dynamicview.theme.ThemeWrapper
import io.snabble.sdk.sample.R

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CouponImageWidget(modifier: Modifier = Modifier, url: String?) {
    GlideSubcomposition(
        modifier = modifier,
        requestBuilderTransform = {
            it.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        },
        model = url
    ) {
        when (this.state) {
            RequestState.Failure -> {
                CouponPlaceholderWidget(modifier = Modifier.fillMaxWidth())
            }

            RequestState.Loading -> {
                LoadingScreen(
                    modifier = Modifier
                        .height(160.dp)
                        .aspectRatio(ratio = 16f / 9f)
                        .padding(horizontal = 16.dp)
                )
            }

            is RequestState.Success -> {
                Image(
                    modifier = Modifier
                        .height(160.dp)
                        .aspectRatio(ratio = 16f / 9f)
                        .padding(horizontal = 16.dp),
                    painter = painter,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun CouponPlaceholderWidget(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            Modifier
                .size(160.dp)
                .padding(16.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Image(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center),
                painter = painterResource(id = R.drawable.ic_coupon_placeholder),
                contentDescription = null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CouponPlaceholderPreviewLight() {
    ThemeWrapper {
        CouponPlaceholderWidget()
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CouponPlaceholderPreviewDark() {
    ThemeWrapper {
        CouponPlaceholderWidget()
    }
}
