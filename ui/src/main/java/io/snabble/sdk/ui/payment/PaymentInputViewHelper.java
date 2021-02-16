package io.snabble.sdk.ui.payment;

import android.os.Bundle;

import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.Project;
import io.snabble.sdk.ui.SnabbleUI;

public class PaymentInputViewHelper {
    public static void openPaymentInputView(PaymentMethod paymentMethod, Project project) {
        SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            Bundle args = new Bundle();
            switch (paymentMethod) {
                case VISA:
                    args.putString(CreditCardInputView.ARG_PROJECT_ID, project.getId());
                    args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, PaymentMethod.VISA);
                    callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, args);
                    break;
                case AMEX:
                    args.putString(CreditCardInputView.ARG_PROJECT_ID, project.getId());
                    args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, PaymentMethod.AMEX);
                    callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, args);
                    break;
                case MASTERCARD:
                    args.putString(CreditCardInputView.ARG_PROJECT_ID, project.getId());
                    args.putSerializable(CreditCardInputView.ARG_PAYMENT_TYPE, PaymentMethod.MASTERCARD);
                    callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, args);
                    break;
                case PAYDIREKT:
                    callback.execute(SnabbleUI.Action.SHOW_PAYDIREKT_INPUT, null);
                    break;
                case DE_DIRECT_DEBIT:
                    callback.execute(SnabbleUI.Action.SHOW_SEPA_CARD_INPUT, null);
                    break;
            }
        }
    }
}
