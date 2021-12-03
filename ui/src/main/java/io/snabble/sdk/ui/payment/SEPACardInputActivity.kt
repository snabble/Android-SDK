package io.snabble.sdk.ui.payment

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.snabble.sdk.PaymentOriginCandidateHelper

class SEPACardInputActivity : AppCompatActivity() {
    companion object {
        const val ARG_PAYMENT_ORIGIN_CANDIDATE = "paymentOriginCandidate"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = SEPACardInputView(this)
        view.setOnCloseListener {
            finish()
        }

        val args = intent.extras
        val paymentOriginCandidate = args?.getSerializable(ARG_PAYMENT_ORIGIN_CANDIDATE)
                as? PaymentOriginCandidateHelper.PaymentOriginCandidate

        paymentOriginCandidate?.let {
            view.setPrefilledPaymentOriginCandidate(paymentOriginCandidate)
        }

        setContentView(view)
    }
}