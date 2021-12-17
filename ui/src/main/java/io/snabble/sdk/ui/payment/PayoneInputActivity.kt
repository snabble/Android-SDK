package io.snabble.sdk.ui.payment

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class PayoneInputActivity : BaseFragmentActivity() {
    companion object {
        const val ARG_PROJECT_ID = PayoneInputView.ARG_PROJECT_ID
        const val ARG_PAYMENT_TYPE = PayoneInputView.ARG_PAYMENT_TYPE
        const val ARG_TOKEN_DATA = PayoneInputView.ARG_TOKEN_DATA
    }

    override fun onCreateFragment(): Fragment {
        val fragment = PayoneInputFragment()
        fragment.arguments = intent.extras
        return fragment
    }
}