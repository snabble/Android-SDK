package io.snabble.sdk.ui.payment.payone.sepa.form

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity
import io.snabble.sdk.ui.checkout.PaymentOriginCandidateHelper.PaymentOriginCandidate
import io.snabble.sdk.ui.utils.serializableExtra

class PayoneSepaActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment =
        PayoneSepaFormFragment.createFragment(intent.serializableExtra(ARG_PAYMENT_ORIGIN_CANDIDATE))

    companion object {

        private const val ARG_PAYMENT_ORIGIN_CANDIDATE = "paymentOriginCandidate"

        fun newIntent(
            context: Context,
            paymentOriginCandidate: PaymentOriginCandidate? = null,
        ): Intent = Intent(context, PayoneSepaActivity::class.java).apply {
            paymentOriginCandidate?.let { putExtra(ARG_PAYMENT_ORIGIN_CANDIDATE, it) }
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
    }
}
