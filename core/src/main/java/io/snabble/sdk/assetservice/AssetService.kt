@file:Suppress("TooGenericExceptionCaught")

package io.snabble.sdk.assetservice

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.DisplayMetrics
import com.caverock.androidsvg.SVG
import io.snabble.sdk.Project
import io.snabble.sdk.assetservice.assets.data.AssetsRepositoryImpl
import io.snabble.sdk.assetservice.assets.data.source.LocalAssetDataSourceImpl
import io.snabble.sdk.assetservice.assets.data.source.RemoteAssetsSourceImpl
import io.snabble.sdk.assetservice.assets.domain.AssetsRepository
import io.snabble.sdk.assetservice.image.data.ImageRepositoryImpl
import io.snabble.sdk.assetservice.image.data.local.image.LocalDiskDataSourceImpl
import io.snabble.sdk.assetservice.image.data.local.image.LocalMemorySourceImpl
import io.snabble.sdk.assetservice.image.domain.ImageRepository
import io.snabble.sdk.assetservice.image.domain.model.Type
import io.snabble.sdk.assetservice.image.domain.model.UiMode
import io.snabble.sdk.utils.Logger
import java.io.InputStream
import kotlin.math.roundToInt

interface AssetService {

    suspend fun updateAllAssets()

    suspend fun loadAsset(name: String, type: Type, uiMode: UiMode): Bitmap?
}

internal class AssetServiceImpl(
    private val displayMetrics: DisplayMetrics,
    private val assetRepository: AssetsRepository,
    private val imageRepository: ImageRepository,
) : AssetService {

    /**
     * Updates all assets and safes them locally
     */
    override suspend fun updateAllAssets() {
        assetRepository.updateAllAssets()
    }

    /**
     * Loads an asset and returns it converted as [Bitmap].
     * Bitmap type can be any of these [Type].
     * To define the [UiMode] use the helper function [Context.getUiMode] or set it directly if needed.
     */
    override suspend fun loadAsset(name: String, type: Type, uiMode: UiMode): Bitmap? {
        val bitmap = when (val bitmap = imageRepository.getBitmap(key = name)) {
            null -> createBitmap(name, type, uiMode)
            else -> bitmap
        }

        if (bitmap == null) {
            val newBitmap = updateAssetsAndRetry(name, type, uiMode)
            return newBitmap?.also {
                imageRepository.putBitmap(name, it)
            }
        } else {
            //Save converted bitmap
            imageRepository.putBitmap(name, bitmap)
        }

        return bitmap
    }

    private fun createSVGBitmap(data: InputStream): Bitmap? {
        val svg = SVG.getFromInputStream(data)
        return try {

            val width = svg.getDocumentWidth() * displayMetrics.density
            val height = svg.getDocumentHeight() * displayMetrics.density

            // Set the SVG's view box to the desired size
            svg.setDocumentWidth(width)
            svg.setDocumentHeight(height)

            // Create bitmap and canvas
            val bitmap = androidx.core.graphics.createBitmap(width.roundToInt(), height.roundToInt())
            val canvas = Canvas(bitmap)

            // Render SVG to canvas
            svg.renderToCanvas(canvas)

            bitmap
        } catch (e: Exception) {
            Logger.e("Error converting SVG to bitmap", e)
            null
        }
    }

    private suspend fun createBitmap(name: String, type: Type, uiMode: UiMode): Bitmap? {
        val cachedAsset =
            assetRepository.loadAsset(name = name, type = type, uiMode = uiMode) ?: return null
        return when (type) {
            Type.SVG -> createSVGBitmap(cachedAsset.data)
            Type.JPG,
            Type.WEBP -> BitmapFactory.decodeStream(cachedAsset.data)
        }
    }

    private suspend fun updateAssetsAndRetry(name: String, type: Type, uiMode: UiMode): Bitmap? {
        assetRepository.updateAllAssets()
        return createBitmap(name, type, uiMode)
    }
}

fun assetServiceFactory(
    project: Project,
    context: Context
): AssetService {
    val localDiskDataSource = LocalDiskDataSourceImpl(storageDirectory = project.internalStorageDirectory)
    val localMemoryDataSource = LocalMemorySourceImpl()
    val imageRepository = ImageRepositoryImpl(
        localMemoryDataSource = localMemoryDataSource,
        localDiskDataSource = localDiskDataSource
    )

    val localAssetDataSource = LocalAssetDataSourceImpl(project)
    val remoteAssetsSource = RemoteAssetsSourceImpl(project)
    val assetRepository = AssetsRepositoryImpl(
        remoteAssetsSource = remoteAssetsSource,
        localAssetDataSource = localAssetDataSource
    )

    return AssetServiceImpl(
        assetRepository = assetRepository,
        imageRepository = imageRepository,
        displayMetrics = context.resources.displayMetrics
    )
}

fun Context.getUiMode() = if (isDarkMode()) UiMode.NIGHT else UiMode.DAY

// Method 2: Extension function for cleaner usage
private fun Context.isDarkMode(): Boolean {
    return when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO -> false
        else -> false
    }
}
