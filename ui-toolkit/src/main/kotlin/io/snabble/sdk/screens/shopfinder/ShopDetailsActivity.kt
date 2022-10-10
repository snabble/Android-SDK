package io.snabble.sdk.screens.shopfinder

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

/**
 * Default target for SHOW_SHOP_LIST_DETAILS:
 * Used if no UI action is set for this specific Event.
 * Takes the current shop as param and pass it to the ShopDetails fragment.
 * */
class ShopDetailsActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment {
        val fragment = ShopDetailsFragment()
        fragment.arguments = intent.extras
        return fragment
    }
}