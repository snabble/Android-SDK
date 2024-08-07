package io.snabble.sdk.ui.payment.creditcard.datatrans.ui

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class DatatransActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment = DatatransFragment()
        .apply { arguments = intent.extras }

    companion object {

        const val ARG_PROJECT_ID = DatatransFragment.ARG_PROJECT_ID
        const val ARG_PAYMENT_TYPE = DatatransFragment.ARG_PAYMENT_TYPE
    }
}
