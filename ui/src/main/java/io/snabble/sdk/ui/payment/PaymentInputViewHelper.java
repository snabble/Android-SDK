package io.snabble.sdk.ui.payment;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.ui.Keyguard;
import io.snabble.sdk.ui.R;
import io.snabble.sdk.ui.SnabbleUI;
import io.snabble.sdk.ui.utils.KeyguardUtils;
import io.snabble.sdk.ui.utils.UIUtils;

public class PaymentInputViewHelper {
    public static void openPaymentInputView(Context context, PaymentMethod paymentMethod, String projectId) {
        SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            if (KeyguardUtils.isDeviceSecure()) {
                FragmentActivity activity = UIUtils.getHostFragmentActivity(context);
                Keyguard.unlock(activity, new Keyguard.Callback() {
                    @Override
                    public void success() {
                        Bundle args = new Bundle();
                        switch (paymentMethod) {
                            case VISA:
                                args.putString(CreditCardInputView.ARG_PROJECT_ID, projectId);
                                args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, PaymentMethod.VISA);
                                callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, args);
                                break;
                            case AMEX:
                                args.putString(CreditCardInputView.ARG_PROJECT_ID, projectId);
                                args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, PaymentMethod.AMEX);
                                callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, args);
                                break;
                            case MASTERCARD:
                                args.putString(CreditCardInputView.ARG_PROJECT_ID, projectId);
                                args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, PaymentMethod.MASTERCARD);
                                callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, args);
                                break;
                            case PAYDIREKT:
                                callback.execute(SnabbleUI.Action.SHOW_PAYDIREKT_INPUT, null);
                                break;
                            case DATATRANS:
                                Datatrans.registerPaymentInfo(activity);
                                break;
                            case DE_DIRECT_DEBIT:
                                callback.execute(SnabbleUI.Action.SHOW_SEPA_CARD_INPUT, null);
                                break;
                        }
                    }

                    @Override
                    public void error() {

                    }
                });
            } else {
                new AlertDialog.Builder(context)
                        .setMessage(R.string.Snabble_Keyguard_requireScreenLock)
                        .setPositiveButton(R.string.Snabble_OK, null)
                        .setCancelable(false)
                        .show();
            }

        }
    }
}
