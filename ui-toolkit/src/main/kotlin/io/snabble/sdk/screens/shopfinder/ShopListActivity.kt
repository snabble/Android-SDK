package io.snabble.sdk.screens.shopfinder

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

/**
 * Default target for SHOW_SHOP_LIST EVENT.
 * Used if no UI action is set for this specific Event.
 * */
class ShopListActivity : BaseFragmentActivity() {
    override fun onCreateFragment(): Fragment = ShopListFragment()
}