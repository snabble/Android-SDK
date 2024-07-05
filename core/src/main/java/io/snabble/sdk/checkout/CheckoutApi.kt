package io.snabble.sdk.checkout

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.Product
import io.snabble.sdk.coupons.Coupon
import io.snabble.sdk.FulfillmentState
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.shoppingcart.data.cart.BackendCart
import java.io.Serializable
import java.lang.Exception
import java.util.*

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
    fun onNoAvailablePaymentMethodFound()
    fun onInvalidDepositReturnVoucher()
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
    @SerializedName("coupon") COUPON
}

enum class CheckState {
    @SerializedName("unauthorized") UNAUTHORIZED,
    @SerializedName("pending") PENDING,
    @SerializedName("processing")  PROCESSING,
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
    val href: String? = null,
) : Serializable

data class SignedCheckoutInfo(
    val checkoutInfo: JsonObject? = null,
    val signature: String? = null,
    val links: Map<String, Href>? = null,
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
    val price: Price? = null,
    val lineItems: List<LineItem> = emptyList(),
    val violations: List<Violation> = emptyList(),
)

data class Violation(
    val type: String? = null,
    val refersTo: String? = null,
    val message: String? = null,
)

data class LineItem(
    val id: String? = null,
    val amount: Int = 0,
    @SerializedName("couponID")
    val couponId: String? = null,
    val discountID: String? = null,
    val discountRuleID: String? = null,
    val discountType: String? = null,
    val listPrice: Int = 0,
    val referenceUnit: String? = null,
    val name: String? = null,
    val price: Int = 0,
    val priceModifiers: List<PriceModifier>? = null,
    val redeemed: Boolean = false,
    val refersTo: String? = null,
    val scannedCode: String? = null,
    val sku: String? = null,
    val totalPrice: Int = 0,
    val type: LineItemType? = null,
    val units: Int? = null,
    val weight: Int? = null,
    val weightUnit: String? = null,
)

data class PriceModifier(
    val name: String? = null,
    val price: Int = 0,
)

data class ExitToken(
    val value: String? = null,
    val format: String? = null,
)

data class Price(
    val price: Int = 0,
    val netPrice: Int = 0,
)

data class PaymentInformation(
    val qrCodeContent: String? = null,
    val encryptedOrigin: String? = null,
    val originType: String? = null,
    val validUntil: String? = null,
    val cardNumber: String? = null,
    val deviceID: String? = null,
    val deviceName: String? = null,
    val deviceFingerprint: String? = null,
    val deviceIPAddress: String? = null,
    val handoverInformation: String? = null,
    val subject: String? = null
)

data class CheckoutProcessRequest(
    val signedCheckoutInfo: SignedCheckoutInfo? = null,
    val paymentMethod: PaymentMethod? = null,
    val paymentInformation: PaymentInformation? = null,
    val finalizedAt: String? = null,
    val processedOffline: Boolean? = null,
)

data class PaymentMethodInfo(
    val id: String? = null,
    val isTesting: Boolean = false,
    val acceptedOriginTypes: List<String> = emptyList()
)

data class PaymentResult(
    val originCandidateLink: String? = null,
    val failureCause: String? = null,
)

data class AuthorizePaymentRequest(
    val encryptedOrigin: String? = null,
)

data class Check(
    val id: String? = null,
    val links: Map<String, Href>? = null,
    val type: CheckType? = null,
    val requiredAge: Int? = null,
    val performedBy: Performer? = null,
    val state: CheckState? = null,
) {
    val selfLink: String?
        get() = links?.get("self")?.href
}

data class Fulfillment(
    val id: String? = null,
    val type: String? = null,
    val state: FulfillmentState? = null,
    val refersTo: List<String> = emptyList(),
    val links: Map<String, Href>? = null,
) {
    val selfLink: String?
        get() = links?.get("self")?.href
}

data class CheckoutProcessResponse(
    val links: Map<String, Href>? = null,
    val checks: List<Check> = emptyList(),
    @SerializedName("orderID")
    val orderId: String? = null,
    val aborted: Boolean = false,
    val paymentMethod: PaymentMethod? = null,
    val paymentInformation: PaymentInformation? = null,
    val paymentPreauthInformation: JsonObject? = null,
    val exitToken: ExitToken? = null,
    val paymentState: CheckState? = null,
    val pricing: Pricing? = null,
    val routingTarget: RoutingTarget? = null,
    val paymentResult: PaymentResult? = null,
    val fulfillments: List<Fulfillment> = emptyList(),
) {
    val selfLink: String?
        get() = links?.get("self")?.href

    val authorizePaymentLink: String?
        get() = links?.get("authorizePayment")?.href

    val originCandidateLink: String?
        get() = paymentResult?.originCandidateLink
}

data class Pricing(
    val price: Price? = null,
)
