package io.snabble.sdk.screens.onboarding.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.utils.dpInPx

/** Hides and resizes the layout on overlap with other layouts */
class HideWhenTinyFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    var minHeight = 0

    init {
        attrs?.let {
            context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.HideWhenTinyFrameLayout,
                0, 0).apply {

                try {
                    minHeight = getDimensionPixelSize(R.styleable.HideWhenTinyFrameLayout_minHeight, 0)
                } finally {
                    recycle()
                }
            }
        }

        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            isVisible = height > minHeight
        }
    }
}