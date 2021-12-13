package io.snabble.sdk.ui.payment

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.SimpleFragmentActivity

class CreditCardInputActivity : SimpleFragmentActivity() {
    companion object {
        const val ARG_PROJECT_ID = CreditCardInputView.ARG_PROJECT_ID
        const val ARG_PAYMENT_TYPE = CreditCardInputView.ARG_PAYMENT_TYPE
    }

    override fun onCreateFragment(): Fragment {
        val fragment = CreditCardInputFragment()
        fragment.arguments = intent.extras
        return fragment
    }
}