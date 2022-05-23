package io.snabble.sdk.ui.coupon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.snabble.sdk.CouponType
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.loadImage

open class CouponFragment : Fragment() {
    companion object {
        const val ARG_COUPON = "coupon"
    }

    private val coupon by lazy { arguments?.getParcelable(ARG_COUPON) as? Coupon }
    private val project by lazy { Snabble.getProjectById(coupon?.projectId) }

    private lateinit var header: ImageView
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var description: TextView
    private lateinit var discount: TextView
    private lateinit var expire: TextView
    private lateinit var redeemForScanGo: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.snabble_fragment_coupon, container, false).apply {
        header = findViewById(R.id.header)
        title = findViewById(R.id.title)
        subtitle = findViewById(R.id.subtitle)
        description = findViewById(R.id.description)
        discount = findViewById(R.id.discount)
        expire = findViewById(R.id.expire)
        redeemForScanGo = findViewById(R.id.redeemForScanGo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val coupon = this.coupon ?: return
        val project = this.project ?: return

        header.loadImage(coupon.imageURL)
        header.setBackgroundColor(coupon.backgroundColor)
        title.text = coupon.title
        subtitle.text = coupon.subtitle
        description.text = coupon.disclaimer
        discount.text = coupon.text
        expire.text = coupon.buildExpireString(resources)

        val sdkCoupon = project.coupons.filter(CouponType.DIGITAL).firstOrNull { it.id == coupon.id }

        project.shoppingCart.let { cart ->
            for (i in 0 until cart.size()) {
                val cartCoupon = cart.get(i).coupon
                if (cartCoupon != null && sdkCoupon != null) {
                    if (cartCoupon.id == sdkCoupon.id) {
                        redeem()
                        break
                    }
                }
            }
        }

        redeemForScanGo.setOnClickListener {
            sdkCoupon?.let {
                project.shoppingCart.addCoupon(sdkCoupon)
                redeem()
            }
        }
    }

    private fun redeem() {
        redeemForScanGo.isVisible = true
        redeemForScanGo.setText(R.string.Snabble_Coupon_activated)
        redeemForScanGo.isEnabled = false
    }
}