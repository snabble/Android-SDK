package io.snabble.sdk.shopfinder

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class ShopListActivity : BaseFragmentActivity() {
    override fun onCreateFragment(): Fragment = ShopListFragment()
}