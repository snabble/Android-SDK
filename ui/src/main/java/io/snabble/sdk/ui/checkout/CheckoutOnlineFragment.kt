package io.snabble.sdk.ui.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.R

open class CheckoutOnlineFragment : Fragment() {
    private var view: CheckoutOnlineView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        view = inflater.inflate(
            R.layout.snabble_fragment_checkout_online,
            container,
            false
        ) as CheckoutOnlineView
        return view
    }
}