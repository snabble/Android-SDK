package io.snabble.sdk.ui.payment

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.SimpleFragmentActivity

class PaydirektInputActivity : SimpleFragmentActivity() {
    override fun onCreateFragment(): Fragment = PaydirektInputFragment()
}