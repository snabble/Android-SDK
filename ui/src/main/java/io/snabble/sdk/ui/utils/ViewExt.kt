@file:JvmName("ViewUtils")
package io.snabble.sdk.ui.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.snabble.sdk.Assets
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import java.util.*

fun View.executeUiAction(event: SnabbleUI.Event, args: Bundle? = null) {
    SnabbleUI.executeAction(context, event, args)
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

inline val View.idName: String
    get() = if (id == -1) "null" else context.resources.getResourceName(id)

inline var View.behavior: CoordinatorLayout.Behavior<*>?
    get() = (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior
    set(value) { (layoutParams as? CoordinatorLayout.LayoutParams)?.behavior = value }

fun TextView.setTextOrHide(text: CharSequence?) {
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

inline val Number.dpInPx: Int
    get() = dp.toInt()

inline val Number.dp: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toFloat(), Resources.getSystem().displayMetrics)

inline val Number.spInPx: Int
    get() = sp.toInt()

inline val Number.sp: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, toFloat(), Resources.getSystem().displayMetrics)

fun RecyclerView.ViewHolder.getString(@StringRes string: Int, vararg args: Any?) =
    itemView.resources.getString(string, *args)

interface Sides {
    var top: Int
    var left: Int
    var right: Int
    var bottom: Int
}

inline val View.padding: Sides
    get() = object : Sides {
        override var top: Int
            get() = this@padding.paddingTop
            set(value) = this@padding.setPadding(left, value, right, bottom)
        override var left: Int
            get() = this@padding.paddingLeft
            set(value) = this@padding.setPadding(value, top, right, bottom)
        override var right: Int
            get() = this@padding.paddingRight
            set(value) = this@padding.setPadding(left, top, value, bottom)
        override var bottom: Int
            get() = this@padding.paddingBottom
            set(value) = this@padding.setPadding(left, top, right, value)
    }

inline val View.margin: Sides
    get() = object : Sides {
        private val params = (layoutParams as? ViewGroup.MarginLayoutParams)
        override var top: Int
            get() = params?.topMargin ?: 0
            set(value) { params?.topMargin = value }
        override var left: Int
            get() = params?.leftMargin ?: 0
            set(value) { params?.leftMargin = value }
        override var right: Int
            get() = params?.rightMargin ?: 0
            set(value) { params?.rightMargin = value }
        override var bottom: Int
            get() = params?.bottomMargin ?: 0
            set(value) { params?.bottomMargin = value }
    }

fun ImageView.loadImage(url: String?) = url?.let { Picasso.get().load(url).into(this) }