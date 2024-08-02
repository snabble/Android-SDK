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
import io.snabble.sdk.ui.payment.externalbilling.ExternalBillingFragment.Companion.ARG_PROJECT_ID
import io.snabble.sdk.ui.utils.KeyguardUtils
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.utils.Logger

object PaymentInputViewHelper {

    @JvmStatic
    fun openPaymentInputView(context: Context, paymentMethod: PaymentMethod?, projectId: String) {
        if (KeyguardUtils.isDeviceSecure()) {
            val project = Snabble.getProjectById(projectId) ?: return
            if (paymentMethod == null) {
                Logger.e("Payment method requires no credentials or is unsupported")
                return
            }
            val acceptedOriginTypes = project.paymentMethodDescriptors
                .firstOrNull { it.paymentMethod == paymentMethod }?.acceptedOriginTypes.orEmpty()
            val useDatatrans = acceptedOriginTypes.any { it == "datatransAlias" || it == "datatransCreditCardAlias" }
            val usePayone = acceptedOriginTypes.any { it == "payonePseudoCardPAN" }
            val useFiserv = acceptedOriginTypes.any { it == "ipgHostedDataID" }

            val activity = UIUtils.getHostFragmentActivity(context)
            val args = Bundle()

            when {
                useDatatrans -> Datatrans.registerCard(activity, project, paymentMethod)
                usePayone -> Payone.registerCard(activity, project, paymentMethod, Snabble.formPrefillData)
                useFiserv -> {
                    args.putString(CreditCardInputView.ARG_PROJECT_ID, projectId)
                    args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, paymentMethod)
                    SnabbleUI.executeAction(context, SnabbleUI.Event.SHOW_CREDIT_CARD_INPUT, args)
                }
                paymentMethod == PaymentMethod.EXTERNAL_BILLING -> {
                    args.putString(ARG_PROJECT_ID, projectId)
                    SnabbleUI.executeAction(context, SnabbleUI.Event.SHOW_EXTERNAL_BILLING, args)
                }

                else -> {
                    val event = when (paymentMethod) {
                        PaymentMethod.GIROPAY -> SnabbleUI.Event.SHOW_GIROPAY_INPUT
                        PaymentMethod.DE_DIRECT_DEBIT -> SnabbleUI.Event.SHOW_SEPA_CARD_INPUT
                        PaymentMethod.PAYONE_SEPA -> SnabbleUI.Event.SHOW_PAYONE_SEPA
                        else -> null
                    } ?: return

                    SnabbleUI.executeAction(context, event, args)
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
        args.putSerializable(
            PaymentCredentialsListView.ARG_PAYMENT_TYPE,
            ArrayList(PaymentCredentials.Type.values().toList())
        )
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
