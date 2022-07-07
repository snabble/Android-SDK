package io.snabble.sdk.ui.scanner

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.squareup.picasso.Picasso
import io.snabble.sdk.Project
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.observeView
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks

class CartButtonView  @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val background by lazy { findViewById<ImageView>(R.id.background) }
    private val badgeBackground by lazy { findViewById<ImageView>(R.id.badge_background) }
    private val badgeText by lazy { findViewById<TextView>(R.id.badge_text) }
    private var project: Project? = null

    private val cartListener = object : ShoppingCart.SimpleShoppingCartListener() {
        override fun onChanged(list: ShoppingCart) {
            updateCartQuantity()
        }
    }

    init {
        inflate(context, R.layout.snabble_view_cart_button, this)

        clipChildren = false
        clipToPadding = false

        if (!isInEditMode) {
            Snabble.checkedInProject.observeView(this) {
                project = it
                registerListeners()
            }
        }

        setOnClickListener {
            SnabbleUI.executeAction(getContext(), SnabbleUI.Event.SHOW_SHOPPING_CART)
        }
    }

    private fun updateCartQuantity() {
        val q = project?.shoppingCart?.totalQuantity ?: 0
        if (q > 0) {
            badgeText.text = q.toString()
            badgeText.isVisible = true
            badgeBackground.isVisible = true
            background.setImageResource(R.drawable.snabble_ic_cart_button_background_cutout)
        } else {
            badgeText.isVisible = false
            badgeBackground.isVisible = false
            background.setImageResource(R.drawable.snabble_ic_cart_button_background)
        }
    }

    fun playAddToCartAnimation() {
        animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
                animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
        }.start()
    }

    private fun registerListeners() {
        project?.shoppingCart?.addListener(cartListener)
        updateCartQuantity()
    }

    private fun unregisterListeners() {
        project?.shoppingCart?.removeListener(cartListener)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!isInEditMode) {
            val application = context.applicationContext as Application
            application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
            registerListeners()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        val application = context.applicationContext as Application
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        unregisterListeners()
    }

    private val activityLifecycleCallbacks: ActivityLifecycleCallbacks =
        object : SimpleActivityLifecycleCallbacks() {
            override fun onActivityStarted(activity: Activity) {
                if (UIUtils.getHostActivity(getContext()) === activity) {
                    registerListeners()
                }
            }

            override fun onActivityStopped(activity: Activity) {
                if (UIUtils.getHostActivity(getContext()) === activity) {
                    unregisterListeners()
                }
            }
        }

}