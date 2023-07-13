package io.snabble.sdk.ui.cart

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

@Deprecated(
    message = "The solo shopping cart view is end-of-life.",
    ReplaceWith(
        "Use the CombinedScannerFragment instead.",
        "io.snabble.sdk.ui.scanner.CombinedScannerFragment"
    )
)
class ShoppingCartActivity : BaseFragmentActivity() {

    override fun onCreateFragment(): Fragment = ShoppingCartFragment()
}
