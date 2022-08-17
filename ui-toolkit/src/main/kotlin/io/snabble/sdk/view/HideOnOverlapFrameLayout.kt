package io.snabble.sdk.view

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.forEach
import androidx.core.view.isVisible
import io.snabble.sdk.ui.toolkit.R


class HideOnOverlapFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    var viewGroupId: Int = -1

    init {
        attrs?.let {
            context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.HideOnOverlapFrameLayout,
                0, 0).apply {

                try {
                    viewGroupId = getResourceId(R.styleable.HideOnOverlapFrameLayout_viewGroupId, -1)
                } finally {
                    recycle()
                }
            }
        }

        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val view = (context as? Activity)?.findViewById<View>(viewGroupId)
            val vg = view as? ViewGroup
            vg?.let {
                val visibleRect = Rect()
                getGlobalVisibleRect(visibleRect)

                it.forEach { child ->
                    val childVisibleRect = Rect()
                    child.getGlobalVisibleRect(childVisibleRect)
                    if (childVisibleRect.intersect(visibleRect)) {
                        isVisible = false
                    }
                }
            }
        }
    }
}