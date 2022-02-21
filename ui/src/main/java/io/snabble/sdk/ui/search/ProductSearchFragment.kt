package io.snabble.sdk.ui.search

import android.os.Bundle
import android.view.View
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R

open class ProductSearchFragment : BaseFragment(R.layout.snabble_fragment_productsearch) {
    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        val v = view as ProductSearchView
        v.allowAnyCode = true
    }
}