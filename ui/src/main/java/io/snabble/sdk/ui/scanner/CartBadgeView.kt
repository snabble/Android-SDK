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
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.ui.utils.observeView
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks

class CartBadgeView  @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val background by lazy { findViewById<ImageView>(R.id.background) }
    val badgeBackground by lazy { findViewById<ImageView>(R.id.badge_background) }
    val badgeText by lazy { findViewById<TextView>(R.id.badge_text) }
    var project: Project? = null

    val cartListener = object : ShoppingCart.SimpleShoppingCartListener() {
        override fun onChanged(list: ShoppingCart) {
            badgeText.text = list.totalQuantity.toString()
        }
    }

    init {
        inflate(context, R.layout.snabble_cart_item_overlay, this)

        Snabble.checkedInProject.observeView(this) {
            registerListeners()
        }
    }

    private fun registerListeners() {
        project?.shoppingCart?.addListener(cartListener)
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