package io.snabble.sdk.ui.cart

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class ShoppingCartActivity : BaseFragmentActivity() {
    override fun onCreateFragment(): Fragment = ShoppingCartFragment()
}