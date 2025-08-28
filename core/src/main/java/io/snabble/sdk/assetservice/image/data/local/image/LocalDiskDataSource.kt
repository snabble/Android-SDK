@file:Suppress("TooGenericExceptionCaught")

package io.snabble.sdk.assetservice.image.data.local.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.snabble.sdk.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

internal interface LocalDiskDataSource {

    suspend fun getBitmap(key: String): Bitmap?
    suspend fun saveToDisk(key: String, bitmap: Bitmap): Any
    suspend fun clearCache()
}

internal class LocalDiskDataSourceImpl(
    private val storageDirectory: File,
) : LocalDiskDataSource {

    private val diskCacheDir: File
    private val cacheScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        diskCacheDir = initDiskCacheDir()
    }

    private fun initDiskCacheDir(): File {
        val cacheDir = File(storageDirectory, DISK_CACHE_SUBDIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        Logger.d("Init disk cache dir: ${cacheDir.absolutePath}")

        // Clean up old cache files if over size limit
        cacheScope.launch {
            cleanupDiskCache()
        }

        return cacheDir
    }

    override suspend fun getBitmap(key: String): Bitmap? = withContext(Dispatchers.IO) {
        val cacheFile = getCacheFile(key)

        return@withContext if (cacheFile.exists()) {
            try {
                FileInputStream(cacheFile).use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    // Update file access time for LRU cleanup
                    cacheFile.setLastModified(System.currentTimeMillis())
                    bitmap
                }
            } catch (e: IOException) {
                Logger.e("Error reading image from disk cache: $key", e)
                // Delete corrupted file
                cacheFile.delete()
                null
            }
        } else {
            null
        }
    }

    private fun getCacheFile(key: String): File {
        val safeFileName = key.toMD5() + CACHE_FILE_EXTENSION
        return File(diskCacheDir, safeFileName)
    }

    override suspend fun saveToDisk(key: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val cacheFile = getCacheFile(key)

        try {
            FileOutputStream(cacheFile).use { outputStream ->
                if (bitmap.compress(Bitmap.CompressFormat.PNG, PNG_COMPRESSION_QUALITY, outputStream)) {
                    Logger.d("Saved image to disk: $key")
                } else {
                    cacheFile.delete()
                }
            }
        } catch (e: IOException) {
            Logger.e("Error writing image to disk cache: $key", e)
            cacheFile.delete()
        }
    }

    override suspend fun clearCache() {
        try {
            diskCacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: IOException) {
            Logger.e("Error clearing disk cache", e)
        }
    }

    private suspend fun cleanupDiskCache() = withContext(Dispatchers.IO) {
        try {
            val files = diskCacheDir.listFiles() ?: return@withContext
            val totalSize = files.sumOf { it.length() }

            if (totalSize > MAX_DISK_CACHE_SIZE) {
                Logger.d("Cleaning disk cache: ${totalSize / (BYTES_TO_MB)}MB")

                // Sort by last modified (oldest first)
                val sortedFiles = files.sortedBy { it.lastModified() }
                var currentSize = totalSize

                for (file in sortedFiles) {
                    if (currentSize <= MAX_DISK_CACHE_SIZE * CACHE_CLEANUP_TARGET_RATIO) break // Keep 80% of max size

                    currentSize -= file.length()
                    file.delete()
                    Logger.d("Deleted old cache file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Logger.e("Error during cache cleanup", e)
        }
    }

    private fun String.toMD5(): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(toByteArray())
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: NoSuchAlgorithmException) {
            Logger.e("MD5 algorithm not available", e)
            hashCode().toString()
        } catch (e: OutOfMemoryError) {
            Logger.e("Out of memory creating MD5", e)
            hashCode().toString()
        }
    }

    companion object {

        // Disk cache configuration
        private const val DISK_CACHE_SUBDIR = "assets/"
        private const val MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024L // 50MB
        private const val CACHE_FILE_EXTENSION = ".cache"
        private const val PNG_COMPRESSION_QUALITY = 100
        private const val BYTES_TO_MB = 1024 * 1024
        private const val CACHE_CLEANUP_TARGET_RATIO = 0.8
    }
}
