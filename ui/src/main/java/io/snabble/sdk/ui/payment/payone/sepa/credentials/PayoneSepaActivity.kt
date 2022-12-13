package io.snabble.sdk.ui.payment.payone.sepa.credentials

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class PayoneSepaActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment = PayoneSepaInputFragment()
}
