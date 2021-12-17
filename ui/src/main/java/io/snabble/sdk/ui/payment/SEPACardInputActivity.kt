package io.snabble.sdk.ui.payment

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.snabble.sdk.PaymentOriginCandidateHelper
import io.snabble.sdk.ui.BaseFragmentActivity

class SEPACardInputActivity : BaseFragmentActivity() {
    companion object {
        const val ARG_PAYMENT_ORIGIN_CANDIDATE = SEPACardInputFragment.ARG_PAYMENT_ORIGIN_CANDIDATE
    }

    override fun onCreateFragment(): Fragment {
        val fragment = SEPACardInputFragment()
        fragment.arguments = intent.extras
        return fragment
    }
}