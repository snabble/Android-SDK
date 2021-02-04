package io.snabble.sdk.ui.utils

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import io.snabble.sdk.Assets
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import java.util.*

fun View.executeUiAction(action: SnabbleUI.Action, data: Any? = null) {
    SnabbleUI.getUiCallback()?.execute(action, data)
}

fun ImageView.loadAsset(assets: Assets, name: String) {
    val randomUUID = UUID.randomUUID().toString()
    setTag(R.id.snabble_asset_load_id, randomUUID)
    assets.get(name, Assets.Callback { bitmap: Bitmap? ->
        if (getTag(R.id.snabble_asset_load_id) === randomUUID) {
            setImageBitmap(bitmap)
        }
    })
}