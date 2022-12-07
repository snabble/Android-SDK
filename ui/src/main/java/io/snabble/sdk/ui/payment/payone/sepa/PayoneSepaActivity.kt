package io.snabble.sdk.ui.payment.payone.sepa

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class PayoneSepaActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment = PayoneSepaFragment()
}
