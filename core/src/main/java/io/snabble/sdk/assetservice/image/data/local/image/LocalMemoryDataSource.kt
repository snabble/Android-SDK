package io.snabble.sdk.assetservice.image.data.local.image

import android.graphics.Bitmap
import android.util.LruCache
import io.snabble.sdk.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

internal interface LocalMemoryDataSource {

    val evictedItems: Flow<Pair<String, Bitmap>?>
    suspend fun getBitmap(key: String): Bitmap?
    suspend fun putBitmap(key: String, bitmap: Bitmap)

    fun clearCache()
}

internal class LocalMemorySourceImpl : LocalMemoryDataSource {

    private val memoryCache: LruCache<String, Bitmap>

    private val _evictedItems: MutableStateFlow<Pair<String, Bitmap>?> = MutableStateFlow(null)
    override val evictedItems: StateFlow<Pair<String, Bitmap>?> = _evictedItems.asStateFlow()

    init {
        memoryCache = initMemoryCache()
    }

    private fun initMemoryCache(): LruCache<String, Bitmap> {
        val cacheSize = calculateMemoryCacheSize()
        Logger.d("Setup memory cache ${cacheSize / KB_TO_MB} MB")

        return object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / BYTES_TO_KB
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: String,
                oldValue: Bitmap,
                newValue: Bitmap?
            ) {
                // When evicted from memory, save to disk asynchronously
                if (evicted) {
                    _evictedItems.update { key to oldValue }
                }
            }
        }
    }

    private fun calculateMemoryCacheSize(): Int {
        val maxMemoryKB = (Runtime.getRuntime().maxMemory() / BYTES_TO_KB).toInt()
        val calculatedCacheKB = maxMemoryKB / MEMORY_CACHE_FRACTION
        val minCacheKB = MIN_CACHE_SIZE_MB * KB_TO_MB
        val maxCacheKB = MAX_CACHE_SIZE_MB * KB_TO_MB
        return calculatedCacheKB.coerceIn(minCacheKB, maxCacheKB)
    }

    override suspend fun getBitmap(key: String): Bitmap? = withContext(Dispatchers.IO) {
        memoryCache.get(key)?.let { bitmap ->
            Logger.d("Load image from memory: $key")
            return@withContext bitmap
        }
    }

    override suspend fun putBitmap(key: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        memoryCache.put(key, bitmap)
        Logger.d("Saving image to memory: $key")
    }

    override fun clearCache() {
        memoryCache.evictAll()
    }

    companion object {

        private const val BYTES_TO_KB = 1024
        private const val KB_TO_MB = 1024
        private const val MEMORY_CACHE_FRACTION = 8  // Use 1/8 of available heap
        private const val MIN_CACHE_SIZE_MB = 4
        private const val MAX_CACHE_SIZE_MB = 64
    }
}
