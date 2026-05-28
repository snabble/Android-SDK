package io.snabble.sdk.checkout

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.FulfillmentState
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Product
import io.snabble.sdk.coupons.Coupon
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.shoppingcart.data.cart.BackendCart
import java.io.Serializable
import java.util.Date

/**
 * Interface for the snabble Checkout API
 *
 * Backend API Documentation:
 * https://docs.snabble.io/docs/api/api_checkout
 */
interface CheckoutApi {
    /**
     * Cancel all operations
     */
    fun cancel()

    fun abort(
        checkoutProcessResponse: CheckoutProcessResponse,
        paymentAbortResult: PaymentAbortResult?
    )

    /**
     * Creates a checkout info with mandatory price calculation and available payment methods.
     * This document can be used to show the real price to the user and it can be used
     * to start a checkout process as input of {@link #createPaymentProcess}
     */
    fun createCheckoutInfo(
        backendCart: BackendCart,
        checkoutInfoResult: CheckoutInfoResult? = null,
        timeout: Long = -1
    )

    /**
     *  Updates an existing payment process
     */
    fun updatePaymentProcess(
        checkoutProcessResponse: CheckoutProcessResponse,
        paymentProcessResult: PaymentProcessResult?
    )

    /**
     * Creates a payment process using stored payment credentials and a signed checkout info from
     * create checkout info.
     */
    fun createPaymentProcess(
        id: String,
        signedCheckoutInfo: SignedCheckoutInfo,
        paymentMethod: PaymentMethod,
        processedOffline: Boolean,
        paymentCredentials: PaymentCredentials?,
        finalizedAt: Date?,
        paymentProcessResult: PaymentProcessResult?
    )

    /**
     * Authorize a payment of a existing checkout process providing payment credentials.
     *
     * Only used for one-time token payments like google pay.
     */
    fun authorizePayment(
        checkoutProcessResponse: CheckoutProcessResponse,
        authorizePaymentRequest: AuthorizePaymentRequest,
        authorizePaymentResult: AuthorizePaymentResult?
    )
}

interface AuthorizePaymentResult {
    fun onSuccess()
    fun onError()
}

interface CheckoutInfoResult {
    fun onSuccess(
        signedCheckoutInfo: SignedCheckoutInfo,
        onlinePrice: Int,
        availablePaymentMethods: List<PaymentMethodInfo>
    )

    fun onNoShopFound()
    fun onInvalidProducts(products: List<Product>)
    fun onInvalidItems(itemIds: List<String>)
    fun onNoAvailablePaymentMethodFound()
    fun onUnknownError()
    fun onConnectionError()
}

interface PaymentProcessResult {
    fun onSuccess(checkoutProcessResponse: CheckoutProcessResponse?, rawResponse: String?)
    fun onError()
    fun onNotFound()
}

interface PaymentAbortResult {
    fun onSuccess()
    fun onError()
}

enum class LineItemType {
    @SerializedName("default") DEFAULT,
    @SerializedName("deposit") DEPOSIT,
    @SerializedName("discount") DISCOUNT,
    @SerializedName("coupon") COUPON,
    @SerializedName("depositReturnVoucher") DEPOSIT_RETURN_VOUCHER,
    @SerializedName("depositReturn") DEPOSIT_RETURN
}

enum class CheckState {
    @SerializedName("unauthorized") UNAUTHORIZED,
    @SerializedName("pending") PENDING,
    @SerializedName("processing")  PROCESSING,
    @SerializedName("authenticating") AUTHENTICATING,
    @SerializedName("successful") SUCCESSFUL,
    @SerializedName("transferred") TRANSFERRED,
    @SerializedName("failed") FAILED
}

enum class CheckType {
    @SerializedName("min_age") MIN_AGE,
    @SerializedName("supervisor_approval") SUPERVISOR
}

enum class Performer {
    @SerializedName("app") APP,
    @SerializedName("supervisor") SUPERVISOR,
    @SerializedName("backend") BACKEND,
    @SerializedName("payment") PAYMENT
}

enum class RoutingTarget {
    @SerializedName("gatekeeper") GATEKEEPER,
    @SerializedName("supervisor") SUPERVISOR,
    @SerializedName("none") NONE
}

data class Href(
    @SerializedName("href") val href: String? = null,
) : Serializable

data class SignedCheckoutInfo(
    @SerializedName("checkoutInfo") val checkoutInfo: JsonObject? = null,
    @SerializedName("signature") val signature: String? = null,
    @SerializedName("links") val links: Map<String, Href>? = null,
) {
    val checkoutProcessLink: String?
        get() = links?.get("checkoutProcess")?.href

    val isRequiringTaxation: Boolean
        get() {
            try {
                if (checkoutInfo?.has("requiredInformation") == true) {
                    val jsonArray = checkoutInfo.getAsJsonArray("requiredInformation")
                    jsonArray?.forEach { element ->
                        val id = element.asJsonObject["id"].asString
                        val hasValue = element.asJsonObject.has("value")
                        if (id == "taxation" && !hasValue) {
                            return true
                        }
                    }
                }
            } catch (e: Exception) {
                return false
            }
            return false
        }

    fun getAvailablePaymentMethods(): List<PaymentMethodInfo> {
        if (checkoutInfo?.has("paymentMethods") == true) {
            val jsonArray = checkoutInfo.getAsJsonArray("paymentMethods")
            if (jsonArray != null) {
                val type = object : TypeToken<List<PaymentMethodInfo>?>() {}.type
                return Gson().fromJson(jsonArray, type)
            }
        }
        return emptyList()
    }

    fun getRedeemedCoupons(availableCoupons: List<Coupon>): List<Coupon> {
        val redeemedCoupons = mutableListOf<Coupon>()
        checkoutInfo?.get("lineItems")?.let {
            it.asJsonArray?.let { jsonArray ->
                val lineItems = Gson().fromJson<List<LineItem>>(
                    jsonArray,
                    object : TypeToken<List<LineItem?>?>() {}.type
                )
                lineItems.forEach { lineItem ->
                    if (lineItem.type == LineItemType.COUPON && lineItem.redeemed) {
                        availableCoupons.forEach { coupon ->
                            if (coupon.id == lineItem.couponId) {
                                redeemedCoupons.add(coupon)
                            }
                        }
                    }
                }
            }
        }
        return redeemedCoupons
    }
}

data class CheckoutInfo(
    @SerializedName("price") val price: Price? = null,
    @SerializedName("lineItems") val lineItems: List<LineItem> = emptyList(),
    @SerializedName("violations") val violations: List<Violation> = emptyList(),
)

data class Violation(
    @SerializedName("type") val type: ViolationType? = null,
    @SerializedName("refersTo") val refersTo: String? = null,
    @SerializedName("message") val message: String? = null,
)

enum class ViolationType {
    @SerializedName("deposit_return_voucher_already_redeemed")
    DEPOSIT_RETURN_ALREADY_REDEEMED,

    @SerializedName("deposit_return_voucher_duplicate")
    DEPOSIT_RETURN_DUPLICATED,

    @SerializedName("coupon_already_voided")
    COUPON_ALREADY_VOIDED,

    @SerializedName("coupon_currently_not_valid")
    COUPON_CURRENTLY_NOT_VALID,

    @SerializedName("coupon_invalid")
    COUPON_INVALID,
}

data class LineItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("amount") val amount: Int = 0,
    @SerializedName("couponID") val couponId: String? = null,
    @SerializedName("discountID") val discountID: String? = null,
    @SerializedName("discountRuleID") val discountRuleID: String? = null,
    @SerializedName("discountType") val discountType: String? = null,
    @SerializedName("listPrice") val listPrice: Int = 0,
    @SerializedName("name") val name: String? = null,
    @SerializedName("price") val price: Int = 0,
    @SerializedName("priceModifiers") val priceModifiers: List<PriceModifier>? = null,
    @SerializedName("redeemed") val redeemed: Boolean = false,
    @SerializedName("refersTo") val refersTo: String? = null,
    @SerializedName("referenceUnit") val referenceUnit: String? = null,
    @SerializedName("scannedCode") val scannedCode: String? = null,
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("totalPrice") val totalPrice: Int = 0,
    @SerializedName("type") val type: LineItemType? = null,
    @SerializedName("units") val units: Int? = null,
    @SerializedName("weight") val weight: Int? = null,
    @SerializedName("weightUnit") val weightUnit: String? = null,
)

data class PriceModifier(
    @SerializedName("name") val name: String? = null,
    @SerializedName("price") val price: Int = 0,
    @SerializedName("action") val action: String? = null
)

data class ExitToken(
    @SerializedName("value") val value: String? = null,
    @SerializedName("format") val format: String? = null,
)

data class Price(
    @SerializedName("price") val price: Int = 0,
    @SerializedName("netPrice") val netPrice: Int = 0,
)

data class PaymentInformation(
    @SerializedName("qrCodeContent") val qrCodeContent: String? = null,
    @SerializedName("encryptedOrigin") val encryptedOrigin: String? = null,
    @SerializedName("originType") val originType: String? = null,
    @SerializedName("validUntil") val validUntil: String? = null,
    @SerializedName("cardNumber") val cardNumber: String? = null,
    @SerializedName("deviceID") val deviceID: String? = null,
    @SerializedName("deviceName") val deviceName: String? = null,
    @SerializedName("deviceFingerprint") val deviceFingerprint: String? = null,
    @SerializedName("deviceIPAddress") val deviceIPAddress: String? = null,
    @SerializedName("handoverInformation") val handoverInformation: String? = null,
    @SerializedName("subject") val subject: String? = null
)

data class CheckoutProcessRequest(
    @SerializedName("signedCheckoutInfo") val signedCheckoutInfo: SignedCheckoutInfo? = null,
    @SerializedName("paymentMethod") val paymentMethod: PaymentMethod? = null,
    @SerializedName("paymentInformation") val paymentInformation: PaymentInformation? = null,
    @SerializedName("finalizedAt") val finalizedAt: String? = null,
    @SerializedName("processedOffline") val processedOffline: Boolean? = null,
)

data class PaymentMethodInfo(
    @SerializedName("id") val id: String? = null,
    @SerializedName("isTesting") val isTesting: Boolean = false,
    @SerializedName("acceptedOriginTypes") val acceptedOriginTypes: List<String> = emptyList()
)

data class PaymentResult(
    @SerializedName("originCandidateLink") val originCandidateLink: String? = null,
    @SerializedName("failureCause") val failureCause: String? = null,
)

data class AuthorizePaymentRequest(
    @SerializedName("encryptedOrigin") val encryptedOrigin: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("countryCode") val countryCode: String? = null,
    @SerializedName("state") val state: String? = null
)

data class Check(
    @SerializedName("id") val id: String? = null,
    @SerializedName("links") val links: Map<String, Href>? = null,
    @SerializedName("type") val type: CheckType? = null,
    @SerializedName("requiredAge") val requiredAge: Int? = null,
    @SerializedName("performedBy") val performedBy: Performer? = null,
    @SerializedName("state") val state: CheckState? = null,
) {
    val selfLink: String?
        get() = links?.get("self")?.href
}

data class Fulfillment(
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("state") val state: FulfillmentState? = null,
    @SerializedName("refersTo") val refersTo: List<String> = emptyList(),
    @SerializedName("links") val links: Map<String, Href>? = null,
) {
    val selfLink: String?
        get() = links?.get("self")?.href
}

data class DepositReturnVoucher(
    @SerializedName("refersTo")
    val refersTo: String,
    @SerializedName("state")
    val state: DepositReturnVoucherState
)

enum class DepositReturnVoucherState {
    @SerializedName("pending")
    PENDING,

    @SerializedName("redeemed")
    REDEEMED,

    @SerializedName("redeemingFailed")
    REDEEMING_FAILED,

    @SerializedName("rolledback")
    ROLLED_BACK,

    @SerializedName("rollbackFailed")
    ROLLBACK_FAILED
}

data class CheckoutProcessResponse(
    @SerializedName("links") val links: Map<String, Href>? = null,
    @SerializedName("checks") val checks: List<Check> = emptyList(),
    @SerializedName("orderID") val orderId: String? = null,
    @SerializedName("depositReturnVouchers") val depositReturnVouchers: List<DepositReturnVoucher>? = null,
    @SerializedName("aborted") val aborted: Boolean = false,
    @SerializedName("paymentMethod") val paymentMethod: PaymentMethod? = null,
    @SerializedName("paymentInformation") val paymentInformation: PaymentInformation? = null,
    @SerializedName("paymentPreauthInformation") val paymentPreauthInformation: JsonObject? = null,
    @SerializedName("exitToken") val exitToken: ExitToken? = null,
    @SerializedName("paymentState") val paymentState: CheckState? = null,
    @SerializedName("pricing") val pricing: Pricing? = null,
    @SerializedName("routingTarget") val routingTarget: RoutingTarget? = null,
    @SerializedName("paymentResult") val paymentResult: PaymentResult? = null,
    @SerializedName("fulfillments") val fulfillments: List<Fulfillment> = emptyList(),
) {
    val selfLink: String?
        get() = links?.get("self")?.href

    val authorizePaymentLink: String?
        get() = links?.get("authorizePayment")?.href

    val paymentRedirect: String?
        get() = links?.get("paymentRedirect")?.href

    val originCandidateLink: String?
        get() = paymentResult?.originCandidateLink
}

data class Pricing(
    @SerializedName("price") val price: Price? = null,
)
