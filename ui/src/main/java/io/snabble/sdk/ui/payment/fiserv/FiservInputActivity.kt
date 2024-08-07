package io.snabble.sdk.ui.payment.fiserv

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class FiservInputActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment = FiservInputFragment()
        .apply { arguments = intent.extras }

    companion object {

        const val ARG_PROJECT_ID = FiservInputView.ARG_PROJECT_ID
        const val ARG_PAYMENT_TYPE = FiservInputView.ARG_PAYMENT_TYPE
    }
}
