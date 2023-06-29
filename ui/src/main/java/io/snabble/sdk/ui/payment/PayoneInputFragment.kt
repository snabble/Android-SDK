package io.snabble.sdk.ui.payment

import android.os.Bundle
import android.view.View
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.parcelableExtra
import io.snabble.sdk.ui.utils.serializableExtra

open class PayoneInputFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_cardinput_payone,
    waitForProject = false
) {
    companion object {
        const val ARG_PROJECT_ID = PayoneInputView.ARG_PROJECT_ID
        const val ARG_PAYMENT_TYPE = PayoneInputView.ARG_PAYMENT_TYPE
        const val ARG_TOKEN_DATA = PayoneInputView.ARG_TOKEN_DATA
    }

    private lateinit var projectId: String
    private lateinit var paymentMethod: PaymentMethod
    private lateinit var tokenizationData: Payone.PayoneTokenizationData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectId = requireNotNull(arguments?.getString(ARG_PROJECT_ID, null))
        paymentMethod = requireNotNull(arguments?.serializableExtra(ARG_PAYMENT_TYPE) as? PaymentMethod)
        tokenizationData = requireNotNull(arguments?.parcelableExtra(ARG_TOKEN_DATA))
    }

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<PayoneInputView>(R.id.user_payment_method_view)
            .load(projectId, paymentMethod, tokenizationData)
    }
}
