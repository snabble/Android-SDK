package io.snabble.sdk.assetservice.image.domain

import android.graphics.Bitmap

interface ImageRepository {

    suspend fun getBitmap(key: String): Bitmap?
    suspend fun putBitmap(key: String, bitmap: Bitmap)
}
