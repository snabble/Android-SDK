package io.snabble.sdk.onboarding

import android.content.Context
import android.text.SpannedString
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.text.getSpans
import androidx.core.view.isVisible
import io.snabble.accessibility.accessibility
import io.snabble.accessibility.isTalkBackActive
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.onboarding.entities.OnboardingItem
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.ui.utils.resolveImageOrHide
import io.snabble.sdk.utils.LinkClickListener
import io.snabble.sdk.utils.resolveTextOrHide
import io.snabble.sdk.utils.setTextOrHide

class OnboardingStepView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {
    private val logo: ImageView
    private val text: TextView
    private val title: TextView
    private val footer: TextView
    private val termsButton: Button

    init {
        inflate(context, R.layout.snabble_view_onboarding_step, this)

        isFocusable = true

        logo = findViewById(R.id.logo)
        text = findViewById(R.id.text)
        title = findViewById(R.id.title)
        footer = findViewById(R.id.footer)
        termsButton = findViewById(R.id.terms_button)

        attrs?.let {
            context.theme.obtainStyledAttributes(attrs, R.styleable.OnboardingStepView, 0, 0)
                .apply {
                    try {
                        val titleText = getText(R.styleable.OnboardingStepView_title)
                        val contentText = getText(R.styleable.OnboardingStepView_text)
                        val footerText = getText(R.styleable.OnboardingStepView_footerText)
                        val image = getResourceId(R.styleable.OnboardingStepView_image, -1)

                        title.setTextOrHide(titleText)
                        text.setTextOrHide(contentText)
                        footer.setTextOrHide(footerText)
                        footer.movementMethod = LinkMovementMethod.getInstance()

                        if (image != -1) {
                            logo.apply {
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

    override fun onAttachedToWindow() {
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        super.onAttachedToWindow()
    }

    private fun showDeeplink(context: Context, url: String) =
        SnabbleUiToolkit.executeAction(
            context,
            SnabbleUiToolkit.Event.SHOW_ONBOARDING_DONE,
            bundleOf(SnabbleUiToolkit.DEEPLINK to url)
        )

    fun bind(data: OnboardingItem) {
        logo.resolveImageOrHide(data.imageSource)
        text.resolveTextOrHide(data.text)
        title.resolveTextOrHide(data.title)
        footer.resolveTextOrHide(data.footer)
        termsButton.resolveTextOrHide(data.termsButtonTitle)

        if (data.link != null) {
            termsButton.setOnClickListener {
                showDeeplink(context, data.link)
            }
        } else {
            termsButton.isVisible = false
        }

        // reset click listeners from last binding
        listOf(footer, text).forEach { view ->
            view.setOnClickListener(null)
            view.setOnLongClickListener(null)
        }

        setupAccessibilityOptimization()
    }

    private fun setupAccessibilityOptimization() {
        // Accessibility optimization:
        // Detect in "footer" and "text" view for text links if there are 2 or less links if so make those links
        // nonclickable and add special "double tap" and "long press" handling as optimization for talkback users.
        if (context.isTalkBackActive) {
            listOf(footer, text).forEach { view -> setupAccessibilityOptimization(view) }
        } else {
            // handle link click inside the App,
            // FIXME: Edge case on Android 7 fix with bottomNavigationBar, remains white after backpress
            listOf(footer, text).forEach { view ->
                view.movementMethod = LinkClickListener { url ->
                    showDeeplink(view.context, url.toString())
                }
            }
        }
    }

    private fun setupAccessibilityOptimization(view: TextView) {
        val spannedString = view.text as? SpannedString ?: return

        // Check it text has links aka URLSpans
        val urls = spannedString.getSpans<URLSpan>()
        if (urls.size !in 1..2) return

        urls
            .map { url ->
                // filter those out with uri
                val start = spannedString.getSpanStart(url)
                val end = spannedString.getSpanEnd(url)
                spannedString.subSequence(start, end) to url.url
            }
            .forEachIndexed { index, (label, url) ->
                // Add the special accessibility handling
                val action = context.getString(
                    R.string.Snabble_Onboarding_Deeplink_accessibility,
                    label
                )
                if (index == 0) {
                    view.accessibility.setClickAction(action) {
                        showDeeplink(context, url)
                    }
                } else {
                    view.accessibility.setLongClickAction(action) {
                        showDeeplink(context, url)
                    }
                }
            }

        // The part to make the links not clickable since we handle it with talkback
        view.apply {
            this.text = view.text.toString()
            movementMethod = null
        }
    }
}
