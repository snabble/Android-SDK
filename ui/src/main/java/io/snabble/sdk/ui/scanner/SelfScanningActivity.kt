package io.snabble.sdk.ui.scanner

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class SelfScanningActivity : BaseFragmentActivity() {
    companion object {
        const val ARG_SHOW_PRODUCT_CODE = SelfScanningFragment.ARG_SHOW_PRODUCT_CODE
    }

    override fun onCreateFragment(): Fragment {
        val fragment = SelfScanningFragment()
        fragment.arguments = intent.extras
        return fragment
    }
}