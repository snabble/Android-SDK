package io.snabble.sdk.shopfinder

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class ShopDetailsActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment {
        val fragment = ShopDetailsFragment()
        fragment.arguments = intent.extras
        return fragment
    }
}