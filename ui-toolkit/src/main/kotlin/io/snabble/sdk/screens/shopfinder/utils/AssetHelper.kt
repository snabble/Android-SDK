package io.snabble.sdk.screens.shopfinder.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.widget.ImageView
import androidx.annotation.DrawableRes
import io.snabble.sdk.Assets
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.Dispatch
import java.util.*

object AssetHelper {

    @JvmStatic
    fun load(assets: Assets?, name: String, imageView: ImageView) = assets?.let {

        load(imageView.context.resources, assets, name, Assets.Type.SVG, imageView, false, 0, false)
    }

    @JvmStatic
    fun load(
        res: Resources,
        assets: Assets,
        name: String,
        type: Assets.Type,
        imageView: ImageView,
        extendBackground: Boolean,
        @DrawableRes defaultImageResource: Int,
        async: Boolean,
    ) {
        val randomUUID = UUID.randomUUID().toString()
        imageView.setTag(R.id.asset_load_id, randomUUID)
        assets.get(name, type, async) { bitmap: Bitmap? ->
            if (bitmap == null && defaultImageResource != 0) {
                Dispatch.background {
                    val defaultBitmap = BitmapFactory.decodeResource(res, defaultImageResource)
                    Dispatch.mainThread {
                        setImageBitmap(imageView, randomUUID, defaultBitmap, extendBackground)
                    }
                }
            } else {
                bitmap?.let { setImageBitmap(imageView, randomUUID, it, extendBackground) }
            }
        }
    }

    fun setImageBitmap(imageView: ImageView, uuid: String, bitmap: Bitmap, extendBackground: Boolean) {
        if (imageView.getTag(R.id.asset_load_id) == uuid) {
            imageView.setImageBitmap(bitmap)
            if (extendBackground) {
                extend(imageView, bitmap)
            }
        }
    }

    private fun extend(imageView: ImageView, bitmap: Bitmap?) {
        if (bitmap != null) {
            val color = bitmap.getPixel(0, bitmap.height - 1)
            imageView.setBackgroundColor(color)
        } else {
            imageView.setBackgroundColor(Color.TRANSPARENT)
        }
    }
}
