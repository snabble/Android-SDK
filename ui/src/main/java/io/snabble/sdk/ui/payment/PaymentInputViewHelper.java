package io.snabble.sdk.ui.payment;

import io.snabble.sdk.PaymentMethod;
import io.snabble.sdk.ui.SnabbleUI;

public class PaymentInputViewHelper {
    public static void openPaymentInputView(PaymentMethod paymentMethod) {
        SnabbleUI.Callback callback = SnabbleUI.getUiCallback();
        if (callback != null) {
            switch (paymentMethod) {
                case VISA:
                    CreditCardInputView.type = PaymentMethod.VISA;
                    callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, null);
                    break;
                case AMEX:
                    CreditCardInputView.type = PaymentMethod.AMEX;
                    callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, null);
                    break;
                case MASTERCARD:
                    CreditCardInputView.type = PaymentMethod.MASTERCARD;
                    callback.execute(SnabbleUI.Action.SHOW_CREDIT_CARD_INPUT, null);
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
