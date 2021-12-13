package io.snabble.sdk.ui.payment

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.SimpleFragmentActivity

class ProjectPaymentOptionsActivity : SimpleFragmentActivity() {
    companion object {
        const val ARG_BRAND = ProjectPaymentOptionsView.ARG_BRAND
    }

    override fun onCreateFragment(): Fragment {
        val fragment = ProjectPaymentOptionsFragment()
        fragment.arguments = intent.extras
        return fragment
    }
}