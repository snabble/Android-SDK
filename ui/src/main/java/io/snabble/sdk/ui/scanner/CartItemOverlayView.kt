package io.snabble.sdk.ui.scanner

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.squareup.picasso.Picasso
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ui.R

class CartItemOverlayView  @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val name by lazy { findViewById<TextView>(R.id.name) }
    val price by lazy { findViewById<TextView>(R.id.price) }
    val image by lazy { findViewById<ImageView>(R.id.image) }
    val plus by lazy { findViewById<View>(R.id.plus) }
    val minus by lazy { findViewById<View>(R.id.minus) }
    val quantity by lazy { findViewById<TextView>(R.id.quantity) }

    interface OnRemovedFromCartListener {
        fun onDismiss()
    }

    var onRemovedFromCartListener: OnRemovedFromCartListener? = null

    init {
        inflate(context, R.layout.snabble_cart_item_overlay, this)
    }

    var cartItem: ShoppingCart.Item? = null
        set(value) {
            field = value

            value?.let { cartItem ->
                name.text = cartItem.displayName
                price.text = cartItem.priceText
                minus.setOnClickListener {
                    cartItem.quantity -= 1
                    onRemovedFromCartListener?.onDismiss()
                }
                plus.setOnClickListener {
                    cartItem.quantity += 1
                }
                val imageUrl = cartItem.product?.imageUrl
                if (imageUrl != null) {
                    image.isVisible = true
                    Picasso.get().load(imageUrl)
                } else {
                    image.isVisible = false
                }
            }
        }
}