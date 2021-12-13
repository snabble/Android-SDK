package io.snabble.sdk.ui.search

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.SimpleFragmentActivity

class ProductSearchActivity : SimpleFragmentActivity() {
    override fun onCreateFragment(): Fragment = ProductSearchFragment()
}