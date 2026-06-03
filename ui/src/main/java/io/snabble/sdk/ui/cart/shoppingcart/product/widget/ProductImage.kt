package io.snabble.sdk.ui.cart.shoppingcart.product.widget

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
internal fun ProductImage(
    imageUrl: String?,
    contentDescription: String?,
    showPlaceholder: Boolean,
    isAgeRestricted: Boolean,
    age: Int
) {
    if (imageUrl == null && !showPlaceholder && !(isAgeRestricted && age > 0)) return

    Box(modifier = Modifier.wrapContentSize()) {
        if (imageUrl != null) {
            var hasFailed by remember(imageUrl) { mutableStateOf(false) }

            if (hasFailed) return

            GlideImage(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                model = imageUrl,
                contentDescription = contentDescription,
            ) { requestBuilder ->
                requestBuilder.addListener(
                    object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean,
                        ): Boolean {
                            hasFailed = true
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean,
                        ): Boolean = false
                    }
                )
            }
        } else if (showPlaceholder) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
        AgeRestrictionIcon(isAgeRestricted, age)
    }
}
