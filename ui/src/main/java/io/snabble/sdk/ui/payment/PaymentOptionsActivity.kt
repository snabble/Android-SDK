package io.snabble.sdk.ui.payment

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class PaymentOptionsActivity : BaseFragmentActivity() {
    override fun onCreateFragment(): Fragment = PaymentOptionsFragment()
}