package io.snabble.sdk.onboarding

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.setTextOrHide

class OnboardingStepView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {
    init {
        inflate(context, R.layout.snabble_view_onboarding_step, this)

        isFocusable = true

        attrs?.let {
            context.theme.obtainStyledAttributes(attrs, R.styleable.OnboardingStepView, 0, 0).apply {
                try {
                    val titleText = getText(R.styleable.OnboardingStepView_title)
                    val contentText = getText(R.styleable.OnboardingStepView_text)
                    val footerText = getText(R.styleable.OnboardingStepView_footerText)
                    val image = getResourceId(R.styleable.OnboardingStepView_image, -1)

                    findViewById<TextView>(R.id.title).setTextOrHide(titleText)
                    findViewById<TextView>(R.id.text).setTextOrHide(contentText)
                    val footer = findViewById<TextView>(R.id.footer)
                    footer.setTextOrHide(footerText)
                    footer.movementMethod = LinkMovementMethod.getInstance()

                    if (image != -1) {
                        findViewById<ImageView>(R.id.image).apply {
                            setImageResource(image)
                            isVisible = true
                        }
                    }
                } finally {
                    recycle()
                }
            }
        }
    }
}