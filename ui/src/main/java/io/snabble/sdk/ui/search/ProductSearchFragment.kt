package io.snabble.sdk.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R

open class ProductSearchFragment : BaseFragment() {
    override fun onCreateActualView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.snabble_fragment_productsearch, container, false) as ProductSearchView
        v.allowAnyCode = true
        return v
    }
}