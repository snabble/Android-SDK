package io.snabble.sdk.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R

open class CreditCardInputFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_cardinput_creditcard,
    waitForProject = false
) {
    companion object {
        const val ARG_PROJECT_ID = CreditCardInputView.ARG_PROJECT_ID
        const val ARG_PAYMENT_TYPE = CreditCardInputView.ARG_PAYMENT_TYPE
    }

    var projectId: String? = null
    var paymentMethod: PaymentMethod? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectId = arguments?.getString(ARG_PROJECT_ID, null)
        paymentMethod = arguments?.getSerializable(ARG_PAYMENT_TYPE) as PaymentMethod?
    }

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        val v = view as CreditCardInputView
        v.load(projectId, paymentMethod)
    }
}