package io.snabble.sdk.ui.payment.externalbilling

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity
import io.snabble.sdk.ui.payment.externalbilling.ExternalBillingFragment.Companion.ARG_PROJECT_ID

class ExternalBillingActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment =
        ExternalBillingFragment.createFragment(intent.extras?.getString(ARG_PROJECT_ID))
}
