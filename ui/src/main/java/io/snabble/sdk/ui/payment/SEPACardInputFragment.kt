package io.snabble.sdk.ui.payment

import android.os.Bundle
import android.view.View
import io.snabble.sdk.PaymentOriginCandidateHelper
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R

open class SEPACardInputFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_cardinput_sepa,
    waitForProject = false
) {
    companion object {
        const val ARG_PAYMENT_ORIGIN_CANDIDATE = "paymentOriginCandidate"
    }

    var paymentOriginCandidate: PaymentOriginCandidateHelper.PaymentOriginCandidate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentOriginCandidate = arguments?.getSerializable(SEPACardInputActivity.ARG_PAYMENT_ORIGIN_CANDIDATE)
                as? PaymentOriginCandidateHelper.PaymentOriginCandidate
    }

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        val v = view as SEPACardInputView
        paymentOriginCandidate?.let {
            v.setPrefilledPaymentOriginCandidate(paymentOriginCandidate)
        }
    }
}