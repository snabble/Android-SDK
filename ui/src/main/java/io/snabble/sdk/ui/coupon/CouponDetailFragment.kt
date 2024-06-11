package io.snabble.sdk.ui.coupon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.snabble.sdk.Snabble
import io.snabble.sdk.coupons.Coupon
import io.snabble.sdk.coupons.CouponType
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.loadImage
import io.snabble.sdk.ui.utils.parcelableExtra
import io.snabble.sdk.ui.utils.setTextOrHide

open class CouponDetailFragment : Fragment() {
    companion object {

        const val ARG_COUPON = "coupon"

        fun createCouponDetailFragment(coupon: CouponItem) = CouponDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_COUPON, coupon)
            }
        }
    }

    protected val item by lazy {
        requireNotNull(arguments?.parcelableExtra<CouponItem>(ARG_COUPON)) {
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
    private lateinit var activateCoupon: Button
    private lateinit var appliedCoupon: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.snabble_fragment_coupon_detail, container, false).apply {
        header = findViewById(R.id.header)
        title = findViewById(R.id.title)
        subtitle = findViewById(R.id.subtitle)
        description = findViewById(R.id.description)
        discount = findViewById(R.id.discount)
        expire = findViewById(R.id.expire)
        activateCoupon = findViewById(R.id.activateCoupon)
        appliedCoupon = findViewById(R.id.appliedCoupon)
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
            when {
                sdkCoupon == null -> {
                    activateCoupon.setText(R.string.Snabble_Coupons_expired)
                    activateCoupon.isEnabled = false
                }

                isCouponActive(sdkCoupon) -> {
                    markAsApplied()
                }

                else -> {
                    markAsUsable()
                }
            }
        }
    }

    private fun isCouponActive(coupon: Coupon) = project.shoppingCart.isCouponApplied(coupon)

    private fun removeCoupon(coupon: Coupon) {
        project.shoppingCart.removeCoupon(coupon)
        markAsUsable()
    }

    protected open fun onRedeem(sdkCoupon: Coupon) {
        project.shoppingCart.addCoupon(sdkCoupon)
        markAsApplied()
    }

    protected fun markAsApplied() {
        activateCoupon.text = "Deactivate"
        activateCoupon.setOnClickListener {
            item.coupon?.let {
                removeCoupon(it)
            }

        }
    }

    private fun markAsUsable() {
        activateCoupon.setText(R.string.Snabble_Coupon_activate)
        activateCoupon.setOnClickListener {
            item.coupon?.let {
                onRedeem(it)
            }
        }
    }
}
