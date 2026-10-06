package io.snabble.sdk.ui.cart.shoppingcart.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import io.snabble.sdk.Snabble
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume

private const val BYTES_PER_KILOBYTE = 1024

/** Fraction of the available heap to spend on the bitmap cache (1 / [HEAP_CACHE_DIVISOR]). */
private const val HEAP_CACHE_DIVISOR = 8

/** inSampleSize must be a power of two, so it is stepped by this factor. */
private const val SAMPLE_SIZE_STEP = 2

/** No downsampling / smallest valid inSampleSize. */
private const val MIN_SAMPLE_SIZE = 1

private val cacheSizeKilobytes =
    (Runtime.getRuntime().maxMemory() / BYTES_PER_KILOBYTE / HEAP_CACHE_DIVISOR).toInt()

/**
 * In-memory cache for downloaded and downsampled bitmaps. Keyed by url + target size, since the same
 * url may be requested at different resolutions.
 */
private val bitmapCache = object : LruCache<String, Bitmap>(cacheSizeKilobytes) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / BYTES_PER_KILOBYTE
}

/**
 * Downloads [imageUrl] through the authenticated okHttpClient of the given project, so the correct
 * per-project token is attached even when tokens/projects change at runtime.
 *
 * @param projectId the project whose client (and token) to use; falls back to the checked-in project.
 * @param targetSizePx the longest edge the caller intends to render at; used to downsample the bitmap.
 * Pass <= 0 to decode at full resolution.
 * @return the decoded bitmap, or null on any failure (no project, no client, network error, decode error).
 */
internal suspend fun loadRemoteImage(imageUrl: String, projectId: String?, targetSizePx: Int): Bitmap? {
    val resolvedProjectId = projectId ?: Snabble.checkedInProject.value?.id ?: return null
    val cacheKey = "$imageUrl@$targetSizePx"
    bitmapCache.get(cacheKey)?.let { return it }

    val client = Snabble.projects.firstOrNull { it.id == resolvedProjectId }?.okHttpClient ?: return null
    return downloadBitmap(client, imageUrl, targetSizePx)?.also { bitmapCache.put(cacheKey, it) }
}

private suspend fun downloadBitmap(client: OkHttpClient, imageUrl: String, targetSizePx: Int): Bitmap? =
    suspendCancellableCoroutine { continuation ->
        val call = client.newCall(Request.Builder().get().url(imageUrl).build())
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                // Reading the body or decoding may throw (IOException, OutOfMemoryError); swallow it and
                // resume with null so a single bad image degrades to the placeholder instead of leaking
                // the throwable onto OkHttp's thread and hanging the coroutine.
                val bitmap = try {
                    response.use { resp ->
                        if (!resp.isSuccessful) null else decodeSampledBitmap(resp.body.bytes(), targetSizePx)
                    }
                } catch (_: Exception) {
                    null
                } catch (_: OutOfMemoryError) {
                    null
                }
                if (continuation.isActive) continuation.resume(bitmap)
            }
        })
    }

private fun decodeSampledBitmap(bytes: ByteArray, targetSizePx: Int): Bitmap? {
    if (targetSizePx <= 0) return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetSizePx)
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}

private fun calculateInSampleSize(width: Int, height: Int, targetSizePx: Int): Int {
    if (width <= 0 || height <= 0) return MIN_SAMPLE_SIZE
    var sampleSize = MIN_SAMPLE_SIZE
    val longestEdge = maxOf(width, height)
    while (longestEdge / (sampleSize * SAMPLE_SIZE_STEP) >= targetSizePx) {
        sampleSize *= SAMPLE_SIZE_STEP
    }
    return sampleSize
}
