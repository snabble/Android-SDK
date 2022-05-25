package io.snabble.sdk.ui.coupon

import io.snabble.sdk.ui.BaseFragmentActivity

class CouponDetailActivity : BaseFragmentActivity() {
    companion object {
        const val ARG_COUPON = CouponFragment.ARG_COUPON
    }

    override fun onCreateFragment() = CouponFragment().apply {
        arguments = intent.extras
    }
}