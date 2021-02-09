package io.snabble.sdk.ui.integration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.ui.payment.CreditCardInputView
import io.snabble.sdk.ui.payment.PaymentCredentialsListView

class PaymentCredentialsListFragment : Fragment() {
    companion object {
        const val ARG_PAYMENT_TYPES = "paymentTypes"
    }

    var type: PaymentCredentials.Type? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = arguments?.getSerializable(ARG_PAYMENT_TYPES) as PaymentCredentials.Type?
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.snabble_fragment_payment_credentials_list, container, false) as PaymentCredentialsListView
        type?.let { v.show(it) }
        return v
    }
}