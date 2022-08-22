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

class ImageTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    init {
        inflate(context, R.layout.snabble_view_image_hybrid, this)
    }
    private val imageView = findViewById<ImageView>(R.id.image)
    private val textView = findViewById<TextView>(R.id.image_alt_text)

    /**
     * Resolves the string and loads it into either an imageview or textview based on the given string
     * If the string matches an url or a resource id it will be loaded into the imageview.
     * If the string matches a string id the matching string will be displayed inside the Textview
     * Else the plain text will be displayed.
     */
    fun setDataOrHide(string: String?) {
        isVisible = false
        imageView.isVisible = false
        textView.isVisible = false

        if (string.isNotNullOrBlank()) {
            isVisible = true
            if (string!!.startsWith("http")) {
                imageView.isVisible = true
                Picasso.get().load(string).into(imageView)
            } else {
                val imageId = imageView.context.getImageId(string)
                if (imageId != Resources.ID_NULL) {
                    imageView.isVisible = true
                    imageView.setImageResource(imageId)
                } else {
                    textView.resolveTextOrHide(string)
                }
            }
        }
    }
}