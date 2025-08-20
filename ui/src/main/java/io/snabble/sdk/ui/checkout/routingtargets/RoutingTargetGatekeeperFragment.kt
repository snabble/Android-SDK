package io.snabble.sdk.ui.checkout.routingtargets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R

open class RoutingTargetGatekeeperFragment : BaseFragment(R.layout.snabble_fragment_routing_gatekeeper){
    override fun onCreateActualView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateActualView(inflater, container, savedInstanceState).apply {
            this?.fitsSystemWindows = true
        }
    }
}
