package io.snabble.sdk.ui.cart

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.squareup.picasso.Picasso
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.telemetry.Telemetry

class CartItemOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    private var price: TextView
    private var name: TextView
    private var image: ImageView
    private var plus: View
    private var minus: View

    var item: ShoppingCart.Item? = null
        set(value) {
            field = value

            value?.let { item ->
                price.text = item.totalPriceText
                name.text = item.displayName

                val imageUrl = item.product?.imageUrl
                if (imageUrl != null) {
                    image.isVisible = true
                    Picasso.get().load(item.product?.imageUrl)
                } else {
                    image.isVisible = false
                }

                plus.setOnClickListener {
                    item.quantity++
                }

                minus.setOnClickListener {
                    item.quantity--
                }
            }
        }

    init {
        inflate(context, R.layout.snabble_cart_item_overlay, this)

        price = findViewById(R.id.price)
        name = findViewById(R.id.name)
        image = findViewById(R.id.image)
        plus = findViewById(R.id.plus)
        minus = findViewById(R.id.minus)
    }
}