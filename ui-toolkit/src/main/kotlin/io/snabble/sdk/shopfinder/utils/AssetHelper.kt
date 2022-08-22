package io.snabble.sdk.shopfinder.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar
import io.snabble.sdk.Assets
import java.util.*
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.Dispatch

object AssetHelper {
    @JvmStatic
    fun load(assets: Assets?, name: String, imageView: ImageView) = assets?.let {

        load(imageView.context.resources, assets, name, Assets.Type.SVG, imageView, false, 0, false)
    }

    @JvmStatic
    fun load(assets: Assets, name: String, imageView: ImageView, defaultImageResource: Int) {
        load(imageView.context.resources, assets, name, Assets.Type.SVG, imageView, false, defaultImageResource, false)
    }

    @JvmStatic
    fun load(assets: Assets, name: String, toolbar: Toolbar) {
        val randomUUID = UUID.randomUUID().toString()
        toolbar.setTag(R.id.asset_load_id, randomUUID)
        assets.get(name, Assets.Type.SVG, false) { bitmap ->
            if (toolbar.getTag(R.id.asset_load_id) == randomUUID) {
                toolbar.logo = BitmapDrawable(toolbar.context.resources, bitmap)
            }
        }
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
        async: Boolean
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

    fun loadExtend(imageView: ImageView, bitmap: Bitmap?) {
        imageView.setImageBitmap(bitmap)
        extend(imageView, bitmap)
    }

    private fun extend(imageView: ImageView, bitmap: Bitmap?) {
        if (bitmap != null) {
            val color = bitmap.getPixel(0, bitmap.height - 1)
            imageView.setBackgroundColor(color)
        } else {
            imageView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    enum class ComponentPosition(val id: Int) {
        Top(R.id.asset_load_id_top),
        Left(R.id.asset_load_id_left),
        Right(R.id.asset_load_id_right),
        Bottom(R.id.asset_load_id_bottom)
    }

    @JvmStatic
    fun TextView.setComponentDrawable(assets: Assets?, name: String, position: ComponentPosition = ComponentPosition.Left) {
        val randomUUID = UUID.randomUUID().toString()
        setTag(position.id, randomUUID)
        assets?.get(name) { bitmap: Bitmap? ->
            if (getTag(position.id) == randomUUID) {
                val drawable = bitmap?.let {
                    BitmapDrawable(resources, bitmap).apply {
                        setBounds(0, 0, bitmap.width, bitmap.height)
                    }
                }
                when(position) {
                    ComponentPosition.Left -> setCompoundDrawables(drawable, compoundDrawables[1], compoundDrawables[2], compoundDrawables[3])
                    ComponentPosition.Top -> setCompoundDrawables(compoundDrawables[0], drawable, compoundDrawables[2], compoundDrawables[3])
                    ComponentPosition.Right -> setCompoundDrawables(compoundDrawables[0], compoundDrawables[1], drawable, compoundDrawables[3])
                    ComponentPosition.Bottom -> setCompoundDrawables(compoundDrawables[0], compoundDrawables[1], compoundDrawables[2], drawable)
                }
            }
        }
    }
}