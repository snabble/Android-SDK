package io.snabble.sdk.ui.payment

import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.PaymentMethodDescriptor
import io.snabble.sdk.ui.R

val PaymentMethodDescriptor.icon: Int
    get() = when (id) {
        PaymentMethod.QRCODE_POS.id -> R.drawable.snabble_ic_payment_select_pos
        PaymentMethod.QRCODE_OFFLINE.id -> R.drawable.snabble_ic_payment_select_pos
        PaymentMethod.DE_DIRECT_DEBIT.id -> R.drawable.snabble_ic_payment_select_sepa
        PaymentMethod.VISA.id -> R.drawable.snabble_ic_payment_select_visa
        PaymentMethod.MASTERCARD.id -> R.drawable.snabble_ic_mastercard
        PaymentMethod.AMEX.id -> R.drawable.snabble_ic_amex
        PaymentMethod.POST_FINANCE_CARD.id -> R.drawable.snabble_ic_payment_select_postfinance
        PaymentMethod.TWINT.id -> R.drawable.snabble_ic_payment_select_twint
        PaymentMethod.CUSTOMERCARD_POS.id -> R.drawable.snabble_ic_payment_select_pos
        PaymentMethod.GATEKEEPER_TERMINAL.id -> R.drawable.snabble_ic_payment_select_sco
        PaymentMethod.PAYDIREKT.id -> R.drawable.snabble_ic_payment_select_paydirekt
        PaymentMethod.PAYONESEPADATA.id -> R.drawable.snabble_ic_amex //TODO: Update icon
        PaymentMethod.GOOGLE_PAY.id -> R.drawable.snabble_ic_payment_select_gpay
        "externalBilling" -> when (acceptedOriginTypes?.first()) {
            "tegutEmployeeID" -> R.drawable.snabble_ic_payment_select_tegut
            "leinweberCustomerID" -> R.drawable.snabble_ic_payment_select_leinweber
            else -> 0
        }
        else -> 0
    }
