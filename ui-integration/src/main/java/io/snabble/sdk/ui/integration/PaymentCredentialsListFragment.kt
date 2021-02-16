package io.snabble.sdk.ui.integration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.ui.payment.CreditCardInputView
import io.snabble.sdk.ui.payment.PaymentCredentialsListView

class PaymentCredentialsListFragment : Fragment() {
    companion object {
        const val ARG_PAYMENT_TYPE = PaymentCredentialsListView.ARG_PAYMENT_TYPE
        const val ARG_PROJECT_ID = PaymentCredentialsListView.ARG_PROJECT_ID
    }

    var type: PaymentCredentials.Type? = null
    var project: Project? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = arguments?.getSerializable(ARG_PAYMENT_TYPE) as PaymentCredentials.Type?
        val projectId = arguments?.getSerializable(ARG_PROJECT_ID) as String?
        project = Snabble.getInstance().projects.firstOrNull { it.id == projectId }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.snabble_fragment_payment_credentials_list, container, false) as PaymentCredentialsListView
        type?.let { v.show(it, project) }
        return v
    }
}