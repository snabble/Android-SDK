package io.snabble.sdk.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import io.snabble.sdk.sample.coupons.ui.CouponScreen
import io.snabble.sdk.sample.coupons.ui.CouponViewModel
import io.snabble.sdk.sample.coupons.ui.ShowCoupon
import io.snabble.sdk.ui.coupon.CouponDetailActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class CouponFragment : Fragment() {

    private val couponViewModel: CouponViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        ComposeView(requireContext()).apply {
            setContent {
                CouponScreen(couponViewModel)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleCouponEvents()
    }

    private fun handleCouponEvents() {
        lifecycleScope.launch {
            couponViewModel.event.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .filterNotNull()
                .collectLatest {
                    when (it) {
                        is ShowCoupon -> {
                            findNavController().navigate(
                                R.id.fragment_coupon_details,
                                bundleOf(CouponDetailActivity.ARG_COUPON to it.couponItem)
                            )
                            couponViewModel.eventHandled()
                        }
                    }
                }
        }
    }
}
