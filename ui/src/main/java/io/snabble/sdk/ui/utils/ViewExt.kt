package io.snabble.sdk.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.snabble.sdk.Assets
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SimpleFragmentActivity
import io.snabble.sdk.ui.SnabbleUI
import java.util.*

fun View.executeUiAction(action: SnabbleUI.Action,
                         args: Bundle? = null) {
    SnabbleUI.executeAction(context, action, args)
}

fun View.getFragmentActivity(): FragmentActivity? =
    UIUtils.getHostFragmentActivity(context)

fun View.requireFragmentActivity() =
    requireNotNull(getFragmentActivity())

fun Context.getFragmentActivity(): FragmentActivity? =
    UIUtils.getHostFragmentActivity(this)

fun Context.requireFragmentActivity() =
    requireNotNull(getFragmentActivity())

fun ImageView.loadAsset(assets: Assets, name: String) {
    val randomUUID = UUID.randomUUID().toString()
    setTag(R.id.snabble_asset_load_id, randomUUID)
    assets.get(name) { bitmap: Bitmap? ->
        if (getTag(R.id.snabble_asset_load_id) === randomUUID) {
            setImageBitmap(bitmap)
        }
    }
}

fun View.setOneShotClickListener(callback: () -> Unit) =
    setOnClickListener(
        object : OneShotClickListener() {
            override fun click() {
                callback.invoke()
            }
        }
    )

inline var View.marginTop: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin ?: 0
    set(value) {
        (layoutParams as? ViewGroup.MarginLayoutParams)?.topMargin = value
    }

inline var View.marginBottom: Int
    get() = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
    set(value) {
        (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = value
    }

inline val View.idName: String
    get() = context.resources.getResourceName(id)

inline var View.behavior: CoordinatorLayout.Behavior<*>?
    get() = (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
    set(value) { (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior = value }

fun TextView.setOrHide(text: CharSequence?) {
    this.isVisible = text.isNotNullOrBlank()
    this.text = text
}

fun <T> LiveData<T>.observeView(view: View, observer: Observer<T>) {
    view.getFragmentActivity()?.let {
        observe(it, observer)
    }

    view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View?) {
            view.getFragmentActivity()?.let {
                observe(it, observer)
            }
        }

        override fun onViewDetachedFromWindow(v: View?) {
            removeObserver(observer)
        }
    })
}