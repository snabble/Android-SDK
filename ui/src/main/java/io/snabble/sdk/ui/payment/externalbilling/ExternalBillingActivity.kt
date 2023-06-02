package io.snabble.sdk.ui.payment.externalbilling

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class ExternalBillingActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment =
        ExternalBillingFragment.createFragment()
}
