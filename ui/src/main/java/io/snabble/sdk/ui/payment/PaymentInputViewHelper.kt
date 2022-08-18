package io.snabble.sdk.ui.payment

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.KeyguardUtils
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.utils.Logger

object PaymentInputViewHelper {
    @JvmStatic
    fun openPaymentInputView(context: Context, paymentMethod: PaymentMethod?, projectId: String) {
        if (KeyguardUtils.isDeviceSecure()) {
            val project = Snabble.getProjectById(projectId)
            val acceptedOriginTypes = project?.paymentMethodDescriptors
                ?.firstOrNull { it.paymentMethod == paymentMethod }?.acceptedOriginTypes.orEmpty()
            val useDatatrans = acceptedOriginTypes.any { it == "datatransAlias" || it == "datatransCreditCardAlias" }
            val usePayone = acceptedOriginTypes.any { it == "payonePseudoCardPAN" }

            val activity = UIUtils.getHostFragmentActivity(context)
            val args = Bundle()

            if (project != null) {
                if (useDatatrans && paymentMethod != null) {
                    Datatrans.registerCard(activity, project, paymentMethod)
                } else if (usePayone && paymentMethod != null) {
                    Payone.registerCard(activity, project, paymentMethod)
                } else {
                    when (paymentMethod) {
                        PaymentMethod.VISA -> {
                            args.putString(CreditCardInputView.ARG_PROJECT_ID, projectId)
                            args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, PaymentMethod.VISA)
                            SnabbleUI.executeAction(context, SnabbleUI.Event.SHOW_CREDIT_CARD_INPUT, args)
                        }
                        PaymentMethod.AMEX -> {
                            args.putString(CreditCardInputView.ARG_PROJECT_ID, projectId)
                            args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, PaymentMethod.AMEX)
                            SnabbleUI.executeAction(context, SnabbleUI.Event.SHOW_CREDIT_CARD_INPUT, args)
                        }
                        PaymentMethod.MASTERCARD -> {
                            args.putString(CreditCardInputView.ARG_PROJECT_ID, projectId)
                            args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, PaymentMethod.MASTERCARD)
                            SnabbleUI.executeAction(context, SnabbleUI.Event.SHOW_CREDIT_CARD_INPUT, args)
                        }
                        PaymentMethod.PAYDIREKT -> {
                            SnabbleUI.executeAction(context, SnabbleUI.Event.SHOW_PAYDIREKT_INPUT)
                        }
                        PaymentMethod.DE_DIRECT_DEBIT -> {
                            SnabbleUI.executeAction(context, SnabbleUI.Event.SHOW_SEPA_CARD_INPUT)
                        }
                        else -> {
                            Logger.e("Payment method requires no credentials or is unsupported")
                        }
                    }
                }
            }
        } else {
            AlertDialog.Builder(context)
                .setMessage(R.string.Snabble_Keyguard_requireScreenLock)
                .setPositiveButton(R.string.Snabble_ok, null)
                .setCancelable(false)
                .show()
        }
    }

    @JvmStatic
    fun showPaymentList(context: Context, project: Project) {
        val args = Bundle()
        args.putSerializable(PaymentCredentialsListView.ARG_PAYMENT_TYPE,
            ArrayList(PaymentCredentials.Type.values().toList()))
        args.putSerializable(PaymentCredentialsListView.ARG_PROJECT_ID, project.id)
        SnabbleUI.executeAction(context, SnabbleUI.Event.SHOW_PAYMENT_CREDENTIALS_LIST, args)
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
                .setPositiveButton(R.string.Snabble_ok, null)
                .setCancelable(false)
                .show()
        }
    }
}