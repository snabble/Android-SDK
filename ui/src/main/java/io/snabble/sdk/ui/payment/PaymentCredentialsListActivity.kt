package io.snabble.sdk.ui.payment

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class PaymentCredentialsListActivity : BaseFragmentActivity() {
    companion object {
        const val ARG_PAYMENT_TYPE = PaymentCredentialsListView.ARG_PAYMENT_TYPE
        const val ARG_PROJECT_ID = PaymentCredentialsListView.ARG_PROJECT_ID
    }

    override fun onCreateFragment(): Fragment {
        val fragment = PaymentCredentialsListFragment()
        fragment.arguments = intent.extras
        return fragment
    }
}