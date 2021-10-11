package io.snabble.sdk.ui.payment

import android.content.Context
import io.snabble.sdk.ui.payment.Datatrans.registerCard
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.KeyguardUtils
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.utils.UIUtils
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import io.snabble.sdk.Project
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.ui.R
import io.snabble.sdk.utils.Logger

object PaymentInputViewHelper {
    @JvmStatic
    fun openPaymentInputView(context: Context, paymentMethod: PaymentMethod?, projectId: String?) {
        val callback = SnabbleUI.getUiCallback()
        if (callback != null) {
            if (KeyguardUtils.isDeviceSecure()) {
                val project = Snabble.getInstance().getProjectById(projectId)
                val useDatatrans = project?.paymentMethodDescriptors
                    ?.firstOrNull { it.paymentMethod == paymentMethod }?.acceptedOriginTypes
                    ?.any { it == "datatransAlias" || it == "datatransCreditCardAlias" } ?: false

                val activity = UIUtils.getHostFragmentActivity(context)
                val args = Bundle()

                if (useDatatrans && paymentMethod != null) {
                    registerCard(activity, project, paymentMethod)
                } else {
                    when (paymentMethod) {
                        PaymentMethod.VISA -> {
                            args.putString(CreditCardInputView.ARG_PROJECT_ID, projectId)
                            args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, PaymentMethod.VISA)
                            callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, args)
                        }
                        PaymentMethod.AMEX -> {
                            args.putString(CreditCardInputView.ARG_PROJECT_ID, projectId)
                            args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, PaymentMethod.AMEX)
                            callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, args)
                        }
                        PaymentMethod.MASTERCARD -> {
                            args.putString(CreditCardInputView.ARG_PROJECT_ID, projectId)
                            args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, PaymentMethod.MASTERCARD)
                            callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, args)
                        }
                        PaymentMethod.PAYDIREKT -> {
                            callback.execute(SnabbleUI.Action.SHOW_PAYDIREKT_INPUT, null)
                        }
                        PaymentMethod.DE_DIRECT_DEBIT -> {
                            callback.execute(SnabbleUI.Action.SHOW_SEPA_CARD_INPUT, null)
                        }
                        else -> {
                            Logger.e("Payment method requires no credentials or is unsupported")
                        }
                    }
                }
            } else {
                AlertDialog.Builder(context)
                    .setMessage(R.string.Snabble_Keyguard_requireScreenLock)
                    .setPositiveButton(R.string.Snabble_OK, null)
                    .setCancelable(false)
                    .show()
            }
        }
    }

    @JvmStatic
    fun showPaymentList(project: Project) {
        val args = Bundle()
        args.putSerializable(PaymentCredentialsListView.ARG_PAYMENT_TYPE,
            ArrayList(PaymentCredentials.Type.values().toList()))
        args.putSerializable(PaymentCredentialsListView.ARG_PROJECT_ID, project.id)
        SnabbleUI.executeAction(SnabbleUI.Action.SHOW_PAYMENT_CREDENTIALS_LIST, args)
    }

    @JvmStatic
    fun showPaymentSelectionForAdding(context: Context, project: Project) {
        if (KeyguardUtils.isDeviceSecure()) {
            val activity = UIUtils.getHostActivity(context)
            if (activity is FragmentActivity) {
                val dialogFragment = SelectPaymentMethodFragment()
                val args = Bundle()
                args.putString(SelectPaymentMethodFragment.ARG_PROJECT_ID, project.id)
                dialogFragment.arguments = args
                dialogFragment.show(activity.supportFragmentManager, null)
            } else {
                throw RuntimeException("Host activity must be a FragmentActivity")
            }
        } else {
            AlertDialog.Builder(context)
                .setMessage(R.string.Snabble_Keyguard_requireScreenLock)
                .setPositiveButton(R.string.Snabble_OK, null)
                .setCancelable(false)
                .show()
        }
    }
}