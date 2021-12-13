package io.snabble.sdk.ui.cart

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.SimpleFragmentActivity

class ShoppingCartActivity : SimpleFragmentActivity() {
    override fun onCreateFragment(): Fragment = ShoppingCartFragment()
}