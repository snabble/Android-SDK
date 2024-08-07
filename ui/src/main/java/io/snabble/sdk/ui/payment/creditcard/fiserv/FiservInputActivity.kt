package io.snabble.sdk.ui.payment.creditcard.fiserv

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class FiservInputActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment = FiservInputFragment()
        .apply { arguments = intent.extras }

    companion object {

        const val ARG_PROJECT_ID = io.snabble.sdk.ui.payment.creditcard.fiserv.FiservInputView.ARG_PROJECT_ID
        const val ARG_PAYMENT_TYPE = io.snabble.sdk.ui.payment.creditcard.fiserv.FiservInputView.ARG_PAYMENT_TYPE
    }
}
