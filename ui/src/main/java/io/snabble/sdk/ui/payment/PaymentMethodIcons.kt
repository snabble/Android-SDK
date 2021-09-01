package io.snabble.sdk.ui.payment

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.R

fun PaymentMethod.icon() = when(this) {
    PaymentMethod.QRCODE_POS -> R.drawable.snabble_ic_payment_select_pos
    PaymentMethod.QRCODE_OFFLINE -> R.drawable.snabble_ic_payment_select_pos
    PaymentMethod.DE_DIRECT_DEBIT -> R.drawable.snabble_ic_payment_select_sepa
    PaymentMethod.VISA -> R.drawable.snabble_ic_payment_select_visa
    PaymentMethod.MASTERCARD -> R.drawable.snabble_ic_mastercard
    PaymentMethod.AMEX -> R.drawable.snabble_ic_amex
    PaymentMethod.POST_FINANCE_CARD -> R.drawable.snabble_ic_payment_select_postfinance
    PaymentMethod.TWINT -> R.drawable.snabble_ic_payment_select_twint
    PaymentMethod.TEGUT_EMPLOYEE_CARD -> R.drawable.snabble_ic_payment_select_tegut
    PaymentMethod.CUSTOMERCARD_POS -> R.drawable.snabble_ic_payment_select_pos
    PaymentMethod.GATEKEEPER_TERMINAL -> R.drawable.snabble_ic_payment_select_sco
    PaymentMethod.PAYDIREKT -> R.drawable.snabble_ic_payment_select_paydirekt
    PaymentMethod.GOOGLE_PAY -> R.drawable.snabble_ic_payment_select_gpay
}