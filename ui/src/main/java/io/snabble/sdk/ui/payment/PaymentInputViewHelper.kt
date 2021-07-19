package io.snabble.sdk.ui.payment

import android.content.Context
import io.snabble.sdk.ui.payment.Datatrans.Companion.registerCard
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.utils.KeyguardUtils
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.utils.UIUtils
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import io.snabble.sdk.ui.R
import io.snabble.sdk.utils.Logger

object PaymentInputViewHelper {
    @JvmStatic
    fun openPaymentInputView(context: Context?, paymentMethod: PaymentMethod?, projectId: String?) {
        val callback = SnabbleUI.getUiCallback()
        if (callback != null) {
            if (KeyguardUtils.isDeviceSecure()) {
                val project = Snabble.getInstance().getProjectById(projectId)
                val useDatatrans = project?.paymentMethodDescriptors
                    ?.firstOrNull { it.paymentMethod == paymentMethod }?.acceptedOriginTypes
                    ?.any { it == "datatransAlias" } ?: false

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
                AlertDialog.Builder(context!!)
                    .setMessage(R.string.Snabble_Keyguard_requireScreenLock)
                    .setPositiveButton(R.string.Snabble_OK, null)
                    .setCancelable(false)
                    .show()
            }
        }
    }
}