package io.snabble.sdk.onboarding

import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.squareup.picasso.Picasso
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.getImageId
import io.snabble.sdk.utils.isNotNullOrBlank
import io.snabble.sdk.utils.resolveTextOrHide

/**
 * Combined view to show an image or a text.
 * The value can be set with [setDataOrHide], which hides the view when then value is `null`.
 */
class ImageTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    init {
        inflate(context, R.layout.snabble_view_image_hybrid, this)
    }

    private val imageView = findViewById<ImageView>(R.id.image)
    private val textView = findViewById<TextView>(R.id.image_alt_text)

    /**
     * Resolves the string and loads it into either an [ImageView] or [TextView] based on the given string.
     * When the [data] matches an http(s) url or a resource id it will be loaded into the [ImageView].
     * When the [data] matches a string id can be resolved the localized string will be displayed inside the [TextView]
     * otherwise the plain text will be displayed.
     * When [data] is `null` the view will be hidden.
     */
    fun setDataOrHide(data: String?) {
        isVisible = data.isNotNullOrBlank()

        if (data.isNotNullOrBlank()) {
            if (data!!.startsWith("http")) {
                imageView.isVisible = true
                textView.isVisible = false
                Picasso.get().load(data).into(imageView)
            } else {
                val imageId = imageView.context.getImageId(data)
                if (imageId != Resources.ID_NULL) {
                    imageView.isVisible = true
                    imageView.setImageResource(imageId)
                } else {
                    imageView.isVisible = false
                    textView.resolveTextOrHide(data)
                }
            }
        }
    }
}