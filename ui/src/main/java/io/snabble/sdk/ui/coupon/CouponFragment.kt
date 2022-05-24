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
import io.snabble.sdk.coupons.CouponType
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.loadImage
import io.snabble.sdk.ui.utils.setTextOrHide

open class CouponFragment : Fragment() {
    companion object {
        const val ARG_COUPON = "coupon"

        fun createCouponFragment(coupon: Coupon) = CouponFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_COUPON, coupon)
            }
        }
    }

    protected val item by lazy {
        requireNotNull(arguments?.getParcelable(ARG_COUPON) as? CouponItem) {
            "The argument ARG_COUPON is missing or not from type ${CouponItem::javaClass.name}"
        }
    }

    protected val project by lazy {
        requireNotNull(Snabble.getProjectById(item.projectId)) {
            "The passed coupon has no project set, this is not supported."
        }
    }

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
        item.coupon?.let { coupon ->
            header.loadImage(coupon.image?.bestResolutionUrl)
            header.setBackgroundColor(coupon.backgroundColor)
            title.text = coupon.name
            subtitle.text = coupon.description
            description.text = coupon.disclaimer
            discount.text = coupon.promotionDescription
            expire.setTextOrHide(item.buildExpireString(resources))

            val sdkCoupon = project.coupons.filter(CouponType.DIGITAL).firstOrNull { it.id == coupon.id }
            if (sdkCoupon == null) {
                redeemForScanGo.setText(R.string.Snabble_Coupons_expired)
                redeemForScanGo.isEnabled = false
            } else {
                project.shoppingCart.let { cart ->
                    for (i in 0 until cart.size()) {
                        val cartCoupon = cart.get(i).coupon
                        if (cartCoupon != null) {
                            if (cartCoupon.id == sdkCoupon.id) {
                                redeem()
                                break
                            }
                        }
                    }
                }
                redeemForScanGo.setOnClickListener {
                    onRedeem(sdkCoupon)
                }
            }
        }
    }

    protected open fun onRedeem(sdkCoupon: io.snabble.sdk.coupons.Coupon) {
        project.shoppingCart.addCoupon(sdkCoupon)
        redeem()
    }

    protected fun redeem() {
        redeemForScanGo.isVisible = true
        redeemForScanGo.setText(R.string.Snabble_Coupon_activated)
        redeemForScanGo.isEnabled = false
    }
}