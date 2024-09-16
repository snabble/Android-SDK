package io.snabble.sdk.ui.payment

import android.content.Context
import androidx.annotation.DrawableRes
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.PaymentMethod.AMEX
import io.snabble.sdk.PaymentMethod.CUSTOMERCARD_POS
import io.snabble.sdk.PaymentMethod.DE_DIRECT_DEBIT
import io.snabble.sdk.PaymentMethod.EXTERNAL_BILLING
import io.snabble.sdk.PaymentMethod.GATEKEEPER_TERMINAL
import io.snabble.sdk.PaymentMethod.GIROPAY
import io.snabble.sdk.PaymentMethod.GOOGLE_PAY
import io.snabble.sdk.PaymentMethod.MASTERCARD
import io.snabble.sdk.PaymentMethod.PAYONE_SEPA
import io.snabble.sdk.PaymentMethod.POST_FINANCE_CARD
import io.snabble.sdk.PaymentMethod.QRCODE_OFFLINE
import io.snabble.sdk.PaymentMethod.QRCODE_POS
import io.snabble.sdk.PaymentMethod.TEGUT_EMPLOYEE_CARD
import io.snabble.sdk.PaymentMethod.TWINT
import io.snabble.sdk.PaymentMethod.VISA
import io.snabble.sdk.ui.R.drawable.snabble_ic_external_billing
import io.snabble.sdk.ui.R.drawable.snabble_ic_payment_giropay
import io.snabble.sdk.ui.R.drawable.snabble_ic_payment_select_amex
import io.snabble.sdk.ui.R.drawable.snabble_ic_payment_select_gpay
import io.snabble.sdk.ui.R.drawable.snabble_ic_payment_select_mastercard
import io.snabble.sdk.ui.R.drawable.snabble_ic_payment_select_pos
import io.snabble.sdk.ui.R.drawable.snabble_ic_payment_select_postfinance
import io.snabble.sdk.ui.R.drawable.snabble_ic_payment_select_sco
import io.snabble.sdk.ui.R.drawable.snabble_ic_payment_select_sepa
import io.snabble.sdk.ui.R.drawable.snabble_ic_payment_select_tegut
import io.snabble.sdk.ui.R.drawable.snabble_ic_payment_select_twint
import io.snabble.sdk.ui.R.drawable.snabble_ic_payment_select_visa
import io.snabble.sdk.ui.R.string.Snabble_Giropay_title
import io.snabble.sdk.ui.R.string.Snabble_Payment_ExternalBilling_title
import io.snabble.sdk.ui.R.string.Snabble_Payment_payAtCashDesk
import io.snabble.sdk.ui.R.string.Snabble_Payment_payAtSCO

class PaymentMethodMetaDataHelper(
    val context: Context
) {

    private val paymentMethodsMetaData: Map<PaymentMethod, PaymentMethodMetaData> = mapOf(
        GOOGLE_PAY withMeta ("Google Pay" to snabble_ic_payment_select_gpay),
        DE_DIRECT_DEBIT withMeta ("SEPA-Lastschrift" to snabble_ic_payment_select_sepa),
        VISA withMeta ("VISA" to snabble_ic_payment_select_visa),
        MASTERCARD withMeta ("Mastercard" to snabble_ic_payment_select_mastercard),
        AMEX withMeta ("American Express" to snabble_ic_payment_select_amex),
        TWINT withMeta ("Twint" to snabble_ic_payment_select_twint),
        POST_FINANCE_CARD withMeta ("PostFinance Card" to snabble_ic_payment_select_postfinance),
        GIROPAY withMeta (Snabble_Giropay_title.resolved to snabble_ic_payment_giropay),
        PAYONE_SEPA withMeta ("SEPA-Lastschrift" to snabble_ic_payment_select_sepa),
        GATEKEEPER_TERMINAL withMeta (Snabble_Payment_payAtSCO.resolved to snabble_ic_payment_select_sco),
        EXTERNAL_BILLING withMeta (Snabble_Payment_ExternalBilling_title.resolved to snabble_ic_external_billing),
        TEGUT_EMPLOYEE_CARD withMeta ("Tegut... Mitarbeiterkarte" to snabble_ic_payment_select_tegut),
        CUSTOMERCARD_POS withMeta (Snabble_Payment_payAtCashDesk.resolved to snabble_ic_payment_select_pos),
        QRCODE_POS withMeta (Snabble_Payment_payAtCashDesk.resolved to snabble_ic_payment_select_pos),
        QRCODE_OFFLINE withMeta (Snabble_Payment_payAtCashDesk.resolved to snabble_ic_payment_select_pos),
    )

    @DrawableRes
    fun iconFor(paymentMethod: PaymentMethod): Int? = paymentMethodsMetaData[paymentMethod]?.iconResId

    fun labelFor(paymentMethod: PaymentMethod): String? = paymentMethodsMetaData[paymentMethod]?.label

    fun indexOf(paymentMethod: PaymentMethod): Int = paymentMethodsMetaData.keys.indexOf(paymentMethod)

    private val Int.resolved: String get() = context.getString(this)

    private infix fun <T> T.withMeta(that: Pair<String, Int>): Pair<T, PaymentMethodMetaData> =
        Pair(this, PaymentMethodMetaData(iconResId = that.second, label = that.first))
}

data class PaymentMethodMetaData(
    @DrawableRes val iconResId: Int,
    val label: String
)
