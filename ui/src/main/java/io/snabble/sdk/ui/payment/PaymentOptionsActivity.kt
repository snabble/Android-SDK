package io.snabble.sdk.ui.payment

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.SimpleFragmentActivity

class PaymentOptionsActivity : SimpleFragmentActivity() {
    override fun onCreateFragment(): Fragment = PaymentOptionsFragment()
}