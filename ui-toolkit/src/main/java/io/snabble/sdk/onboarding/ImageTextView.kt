package io.snabble.sdk.onboarding

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import io.snabble.sdk.ui.toolkit.R

class ImageTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    init {
        inflate(context, R.layout.snabble_view_image_hybrid, this)
    }
}