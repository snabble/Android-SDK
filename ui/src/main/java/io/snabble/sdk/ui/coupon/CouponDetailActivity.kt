package io.snabble.sdk.ui.coupon

import io.snabble.sdk.ui.BaseFragmentActivity

class CouponDetailActivity : BaseFragmentActivity() {
    companion object {
        const val ARG_COUPON = CouponDetailFragment.ARG_COUPON
    }

    override fun onCreateFragment() = CouponDetailFragment().apply {
        arguments = intent.extras
    }
}