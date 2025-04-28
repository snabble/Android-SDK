package io.snabble.sdk.ui.payment

import android.os.Bundle
import android.view.View
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R

open class PaymentOptionsFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_payment_options,
    waitForProject = false
) {
    companion object {
        const val ARG_PAYMENT_OPTIONS_HEADLINE = PaymentOptionsView.ARG_PAYMENT_OPTIONS_HEADLINE
    }

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        val v = view as PaymentOptionsView
        arguments?.getString(ARG_PAYMENT_OPTIONS_HEADLINE)?.let { v.setHeadline(it) }
    }
}
