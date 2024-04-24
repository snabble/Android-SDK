package io.snabble.sdk

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Nullable
import com.google.gson.annotations.SerializedName

/**
 * Enum class describing payment methods
 */
enum class PaymentMethod(
    /**
     * Unique identifier of the payment method.
     *
     * Matches the unique identifier in the snabble Backend API
     */
    val id: String,
    /**
     * Declares if this payment method can be used while the device is offline.
     */
    val isOfflineMethod: Boolean,
    /**
     * Declares if this payment method needs payment credentials to be added by the user
     * before the payment can continue
     */
    val isRequiringCredentials: Boolean,
    /**
     * Declares if this payment method should be hidden, unless payment credentials are already
     * added by the user
     */
    val isShowOnlyIfCredentialsArePresent: Boolean,
    /**
     * Declares if the payment method can only be aborted with a confirmation by the backend
     */
    val needsAbortConfirmation: Boolean,
) : Parcelable {

    @SerializedName("qrCodePOS")
    QRCODE_POS(
        id = "qrCodePOS",
        isOfflineMethod = false,
        isRequiringCredentials = false,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = false
    ),

    @SerializedName("qrCodeOffline")
    QRCODE_OFFLINE(
        id = "qrCodeOffline",
        isOfflineMethod = true,
        isRequiringCredentials = false,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = false
    ),

    @SerializedName("deDirectDebit")
    DE_DIRECT_DEBIT(
        id = "deDirectDebit",
        isOfflineMethod = false,
        isRequiringCredentials = true,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = true
    ),

    @SerializedName("creditCardVisa")
    VISA(
        id = "creditCardVisa",
        isOfflineMethod = false,
        isRequiringCredentials = true,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = true
    ),

    @SerializedName("creditCardMastercard")
    MASTERCARD(
        id = "creditCardMastercard",
        isOfflineMethod = false,
        isRequiringCredentials = true,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = true
    ),

    @SerializedName("creditCardAmericanExpress")
    AMEX(
        id = "creditCardAmericanExpress",
        isOfflineMethod = false,
        isRequiringCredentials = true,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = true
    ),

    @SerializedName("externalBilling")
    TEGUT_EMPLOYEE_CARD(
        id = "externalBilling",
        isOfflineMethod = false,
        isRequiringCredentials = true,
        isShowOnlyIfCredentialsArePresent = true,
        needsAbortConfirmation = true
    ),

    @SerializedName("customerCardPOS")
    CUSTOMERCARD_POS(
        id = "customerCardPOS",
        isOfflineMethod = false,
        isRequiringCredentials = false,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = false
    ),

    @SerializedName("gatekeeperTerminal")
    GATEKEEPER_TERMINAL(
        id = "gatekeeperTerminal",
        isOfflineMethod = false,
        isRequiringCredentials = false,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = false
    ),

    @SerializedName("paydirektOneKlick")
    GIROPAY(
        id = "paydirektOneKlick",
        isOfflineMethod = false,
        isRequiringCredentials = true,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = true
    ),

    @SerializedName("postFinanceCard")
    POST_FINANCE_CARD(
        id = "postFinanceCard",
        isOfflineMethod = false,
        isRequiringCredentials = true,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = true
    ),

    @SerializedName("twint")
    TWINT(
        id = "twint",
        isOfflineMethod = false,
        isRequiringCredentials = true,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = true
    ),

    @SerializedName("googlePay")
    GOOGLE_PAY(
        id = "googlePay",
        isOfflineMethod = false,
        isRequiringCredentials = false,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = false
    ),

    @SerializedName("deDirectDebit")
    PAYONE_SEPA(
        id = "deDirectDebit",
        isOfflineMethod = false,
        isRequiringCredentials = true,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = true
    ),

    @SerializedName("externalBilling")
    EXTERNAL_BILLING(
        id = "externalBilling",
        isOfflineMethod = false,
        isRequiringCredentials = true,
        isShowOnlyIfCredentialsArePresent = false,
        needsAbortConfirmation = true
    );

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = dest.writeInt(ordinal)

    companion object {

        @JvmField
        val CREATOR = object : Parcelable.Creator<PaymentMethod> {
            override fun createFromParcel(parcel: Parcel) = PaymentMethod.values()[parcel.readInt()]
            override fun newArray(size: Int) = arrayOfNulls<PaymentMethod>(size)
        }

        /**
         * Converts a payment method from its string representation.
         */
        @JvmStatic
        @Nullable
        fun fromString(value: String): PaymentMethod? {
            val values = values()
            for (pm in values) {
                if (pm.id == value) {
                    return pm
                }
            }
            return null
        }

        /**
         * Converts a payment method from its string representation and a list of origins.
         */
        @JvmStatic
        @Nullable
        fun fromIdAndOrigin(id: String, origin: List<String>): PaymentMethod? {
            values().forEach { pm ->
                if (pm.id == id && pm.id == TEGUT_EMPLOYEE_CARD.id) {
                    when (origin[0]) {
                        "tegutEmployeeID" -> return TEGUT_EMPLOYEE_CARD
                        "contactPersonCredentials" -> return EXTERNAL_BILLING
                    }
                } else if (pm.id == id && pm.id == PAYONE_SEPA.id) {
                    //needed for deserialization
                    return when (origin[0]) {
                        "payoneSepaData" -> PAYONE_SEPA
                        else -> DE_DIRECT_DEBIT
                    }
                } else if (pm.id == id) {
                    return pm
                }
            }
            return null
        }
    }
}
