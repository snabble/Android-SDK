package io.snabble.sdk.ui.cart.deprecated

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

@Deprecated("Integrate [io.snabble.sdk.ui.cart.shoppingcart.ShoppingCartScreen] instead.")
class ShoppingCartActivity : BaseFragmentActivity() {
    override fun onCreateFragment(): Fragment = ShoppingCartFragment()
}
