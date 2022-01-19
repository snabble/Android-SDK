package io.snabble.sdk.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.PaymentOriginCandidateHelper
import io.snabble.sdk.ui.R

open class SEPACardInputFragment : Fragment() {
    companion object {
        const val ARG_PAYMENT_ORIGIN_CANDIDATE = "paymentOriginCandidate"
    }

    var paymentOriginCandidate: PaymentOriginCandidateHelper.PaymentOriginCandidate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        paymentOriginCandidate = arguments?.getSerializable(SEPACardInputActivity.ARG_PAYMENT_ORIGIN_CANDIDATE)
                as? PaymentOriginCandidateHelper.PaymentOriginCandidate
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.snabble_fragment_cardinput_sepa, container, false) as SEPACardInputView
        paymentOriginCandidate?.let {
            v.setPrefilledPaymentOriginCandidate(paymentOriginCandidate)
        }
        return v
    }
}