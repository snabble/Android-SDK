package io.snabble.sdk.assetservice.data

import android.graphics.Bitmap
import io.snabble.sdk.assetservice.data.local.image.LocalDiskDataSource
import io.snabble.sdk.assetservice.data.local.image.LocalMemoryDataSource
import io.snabble.sdk.assetservice.domain.ImageRepository
import io.snabble.sdk.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageRepositoryImpl(
    private val localMemoryDataSource: LocalMemoryDataSource,
    private val localDiskDataSource: LocalDiskDataSource
) : ImageRepository {

    private val cacheScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        cacheScope.launch {
            localMemoryDataSource.evictedItems.filterNotNull().collect { (key, bitmap) ->
                localDiskDataSource.saveToDisk(key, bitmap)
            }
        }
    }

    override suspend fun getBitmap(key: String): Bitmap? = withContext(Dispatchers.IO) {

        // 1. Check memory cache first (fastest ~0.1ms)
        localMemoryDataSource.getBitmap(key)?.let {
            return@withContext it
        }

        // 2. Check disk cache (medium speed ~5-20ms)
        localDiskDataSource.getBitmap(key)?.let {
            // Save it in memory cache for faster access next time
            localMemoryDataSource.putBitmap(key, it)
            return@withContext it
        }

        Logger.d("Image cache missing")
        return@withContext null
    }

    /**
     * Manually put a bitmap in cache
     */
    override suspend fun putBitmap(key: String, bitmap: Bitmap) {
        localMemoryDataSource.putBitmap(key, bitmap)
        localDiskDataSource.saveToDisk(key, bitmap)
    }

    /**
     * Clear memory cache (disk cache remains)
     */
    fun clearMemoryCache() {
        localMemoryDataSource.clearCache()
        Logger.d("Memory cache cleared")
    }

    /**
     * Clear everything
     */
    suspend fun clearAllCaches() = withContext(Dispatchers.IO) {
        clearMemoryCache()
        localDiskDataSource.clearCache()
        Logger.d("All caches cleared")
    }

    /**
     * Clean up resources - call this in onDestroy
     */
    fun close() {
        cacheScope.cancel()
        Logger.d("Cache manager closed")
    }
}
