package io.snabble.sdk.ui.payment

import android.os.Bundle
import android.view.View
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R

open class PaymentCredentialsListFragment : BaseFragment(
    layoutResId = R.layout.snabble_fragment_payment_credentials_list,
    waitForProject = false
) {
    companion object {
        const val ARG_PAYMENT_TYPE = PaymentCredentialsListView.ARG_PAYMENT_TYPE
        const val ARG_PROJECT_ID = PaymentCredentialsListView.ARG_PROJECT_ID
    }

    var type: ArrayList<PaymentCredentials.Type>? = null
    var project: Project? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = arguments?.getSerializable(ARG_PAYMENT_TYPE) as ArrayList<PaymentCredentials.Type>?
        val projectId = arguments?.getString(ARG_PROJECT_ID)
        project = Snabble.getInstance().projects.firstOrNull { it.id == projectId }
    }

    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        val v = view as PaymentCredentialsListView
        type?.let { v.show(it, project) }
    }
}