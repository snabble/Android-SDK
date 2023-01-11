package io.snabble.sdk.ui.payment

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.checkout.PaymentOriginCandidateHelper.PaymentOriginCandidate
import io.snabble.sdk.ui.utils.serializableExtra

open class SEPACardInputFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_cardinput_sepa,
    waitForProject = false
) {

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments
            ?.serializableExtra<PaymentOriginCandidate>(ARG_PAYMENT_ORIGIN_CANDIDATE)
            ?.let { (view as? SEPACardInputView)?.setPrefilledPaymentOriginCandidate(it) }
    }

    companion object {

        const val ARG_PAYMENT_ORIGIN_CANDIDATE = "paymentOriginCandidate"

        fun createFragment(paymentOriginCandidate: PaymentOriginCandidate? = null): SEPACardInputFragment =
            SEPACardInputFragment().apply {
                arguments = paymentOriginCandidate?.let { bundleOf(ARG_PAYMENT_ORIGIN_CANDIDATE to it) }
            }
    }
}
