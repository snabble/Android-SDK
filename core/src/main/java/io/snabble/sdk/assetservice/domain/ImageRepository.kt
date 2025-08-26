package io.snabble.sdk.assetservice.domain

import android.graphics.Bitmap

interface ImageRepository {

    suspend fun getBitmap(key: String): Bitmap?
    suspend fun putBitmap(key: String, bitmap: Bitmap)
}
