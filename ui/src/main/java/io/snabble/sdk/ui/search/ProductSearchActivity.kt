package io.snabble.sdk.ui.search

import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragmentActivity

class ProductSearchActivity : BaseFragmentActivity() {
    override fun onCreateFragment(): Fragment = ProductSearchFragment()
}