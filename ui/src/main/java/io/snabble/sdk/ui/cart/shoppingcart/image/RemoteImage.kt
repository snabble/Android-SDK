package io.snabble.sdk.ui.cart.shoppingcart.image

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

/**
 * Loads [imageUrl] through the authenticated okHttpClient of the given snabble project and renders it.
 *
 * The caller owns sizing and shape via [modifier] and must state the intended render resolution
 * explicitly via [targetSizePx] (the longest edge, in pixels) so the bitmap can be downsampled to it.
 * [placeholder] is shown while loading and [error] on failure; both default to empty so the space is
 * simply held by [modifier].
 */
@Composable
internal fun RemoteImage(
    imageUrl: String,
    targetSizePx: Int,
    modifier: Modifier = Modifier,
    projectId: String? = null,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable BoxScope.() -> Unit = {},
    error: @Composable BoxScope.() -> Unit = placeholder,
) {
    val state: RemoteImageState by produceState<RemoteImageState>(
        RemoteImageState.Loading, imageUrl, projectId, targetSizePx
    ) {
        value = RemoteImageState.Loading
        val bitmap = loadRemoteImage(imageUrl, projectId, targetSizePx)
        value = if (bitmap != null) RemoteImageState.Success(bitmap) else RemoteImageState.Error
    }

    Box(modifier = modifier) {
        when (val current = state) {
            RemoteImageState.Loading -> placeholder()
            RemoteImageState.Error -> error()
            is RemoteImageState.Success -> Image(
                modifier = Modifier.matchParentSize(),
                bitmap = current.bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = contentScale,
            )
        }
    }
}

private sealed interface RemoteImageState {
    data object Loading : RemoteImageState
    data class Success(val bitmap: Bitmap) : RemoteImageState
    data object Error : RemoteImageState
}
