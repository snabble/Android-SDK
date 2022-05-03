package io.snabble.sdk.checkout

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import io.snabble.sdk.ShoppingCart.BackendCart
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.Product
import io.snabble.sdk.Coupon
import io.snabble.sdk.FulfillmentState
import io.snabble.sdk.PaymentMethod
import java.lang.Exception
import java.util.*

interface CheckoutApi {
    fun cancel()

    fun abort(
        checkoutProcessResponse: CheckoutProcessResponse?,
        paymentAbortResult: PaymentAbortResult?
    )

    fun createCheckoutInfo(
        backendCart: BackendCart, // TODO migrate to kotlin List
        checkoutInfoResult: CheckoutInfoResult? = null,
        timeout: Long = -1
    )

    fun updatePaymentProcess(
        url: String?,
        paymentProcessResult: PaymentProcessResult?
    )

    fun updatePaymentProcess(
        checkoutProcessResponse: CheckoutProcessResponse?,
        paymentProcessResult: PaymentProcessResult?
    )

    fun createPaymentProcess(
        id: String?,
        signedCheckoutInfo: SignedCheckoutInfo?,
        paymentMethod: PaymentMethod?,
        paymentCredentials: PaymentCredentials?,
        processedOffline: Boolean,
        finalizedAt: Date?,
        paymentProcessResult: PaymentProcessResult?
    )

    fun authorizePayment(
        checkoutProcessResponse: CheckoutProcessResponse?,
        authorizePaymentRequest: AuthorizePaymentRequest?,
        authorizePaymentResult: AuthorizePaymentResult?
    )
}

interface AuthorizePaymentResult {
    fun success()
    fun error()
}

interface CheckoutInfoResult {
    fun success(
        signedCheckoutInfo: SignedCheckoutInfo,
        onlinePrice: Int,
        availablePaymentMethods: List<PaymentMethodInfo>
    )

    fun noShop()
    fun invalidProducts(products: List<Product>)
    fun noAvailablePaymentMethod()
    fun invalidDepositReturnVoucher()
    fun unknownError()
    fun connectionError()
}

interface PaymentProcessResult {
    fun success(checkoutProcessResponse: CheckoutProcessResponse?, rawResponse: String?)
    fun error()
}

interface PaymentAbortResult {
    fun success()
    fun error()
}

enum class LineItemType {
    @SerializedName("default") DEFAULT,
    @SerializedName("deposit") DEPOSIT,
    @SerializedName("discount") DISCOUNT,
    @SerializedName("giveaway") GIVEAWAY,
    @SerializedName("coupon") COUPON
}

enum class CheckState {
    @SerializedName("unauthorized") UNAUTHORIZED,
    @SerializedName("pending") PENDING,
    @SerializedName("processing")  PROCESSING,
    @SerializedName("successful") SUCCESSFUL,
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

/*
 * Data structures as defined here:
 *
 * https://github.com/snabble/docs/blob/master/api_checkout.md
 */
data class Href(
    var href: String? = null,
)

data class SignedCheckoutInfo(
    var checkoutInfo: JsonObject? = null,
    var signature: String? = null,
    var links: Map<String, Href>? = null,
) {
    val checkoutProcessLink: String?
        get() = links?.get("checkoutProcess")?.href

    val isRequiringTaxation: Boolean
        get() {
            try {
                if (checkoutInfo?.has("requiredInformation") == true) {
                    val jsonArray = checkoutInfo?.getAsJsonArray("requiredInformation")
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

    fun getAvailablePaymentMethods(): List<PaymentMethodInfo?> {
        if (checkoutInfo?.has("paymentMethods") == true) {
            val jsonArray = checkoutInfo?.getAsJsonArray("paymentMethods")
            if (jsonArray != null) {
                val type = object : TypeToken<List<PaymentMethodInfo?>?>() {}.type
                return Gson().fromJson<List<PaymentMethodInfo>>(jsonArray, type)
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
    var price: Price? = null,
    var lineItems: List<LineItem> = emptyList(),
)

data class LineItem(
    var id: String? = null,
    var refersTo: String? = null,
    @SerializedName("couponID")
    var couponId: String? = null,
    var sku: String? = null,
    var name: String? = null,
    var scannedCode: String? = null,
    var amount: Int = 0,
    var price: Int = 0,
    var units: Int? = null,
    var weight: Int? = null,
    var weightUnit: String? = null,
    var totalPrice: Int = 0,
    var type: LineItemType? = null,
    var priceModifiers: List<PriceModifier>? = null,
    var redeemed: Boolean = false,
)

data class PriceModifier(
    var name: String? = null,
    var price: Int = 0,
)

data class ExitToken(
    var value: String? = null,
    var format: String? = null,
)

data class Price(
    var price: Int = 0,
    var netPrice: Int = 0,
)

data class PaymentInformation(
    var qrCodeContent: String? = null,
    var encryptedOrigin: String? = null,
    var originType: String? = null,
    var validUntil: String? = null,
    var cardNumber: String? = null,
    var deviceID: String? = null,
    var deviceName: String? = null,
    var deviceFingerprint: String? = null,
    var deviceIPAddress: String? = null,
    var handoverInformation: String? = null,
)

data class CheckoutProcessRequest(
    var signedCheckoutInfo: SignedCheckoutInfo? = null,
    var paymentMethod: PaymentMethod? = null,
    var paymentInformation: PaymentInformation? = null,
    var finalizedAt: String? = null,
    var processedOffline: Boolean? = null,
)

data class PaymentMethodInfo(
    var id: String? = null,
    var isTesting: Boolean = false,
    var acceptedOriginTypes: List<String> = emptyList()
)

data class PaymentResult(
    var originCandidateLink: String? = null,
    var failureCause: String? = null,
)

data class AuthorizePaymentRequest(
    var encryptedOrigin: String? = null,
)

data class Check(
    var id: String? = null,
    var links: Map<String, Href>? = null,
    var type: CheckType? = null,
    var requiredAge: Int? = null,
    var performedBy: Performer? = null,
    var state: CheckState? = null,
) {
    val selfLink: String?
        get() = links?.get("self")?.href
}

data class Fulfillment(
    var id: String? = null,
    var type: String? = null,
    var state: FulfillmentState? = null,
    var refersTo: List<String> = emptyList(),
    var links: Map<String, Href>? = null,
) {
    val selfLink: String?
        get() = links?.get("self")?.href
}

data class CheckoutProcessResponse(
    var links: Map<String, Href>? = null,
    var checks: List<Check> = emptyList(),
    var orderId: String? = null,
    var aborted: Boolean = false,
    var paymentMethod: PaymentMethod? = null,
    var paymentInformation: PaymentInformation? = null,
    var paymentPreauthInformation: JsonObject? = null,
    var exitToken: ExitToken? = null,
    var paymentState: CheckState? = null,
    var pricing: Pricing? = null,
    var routingTarget: RoutingTarget? = null,
    var paymentResult: PaymentResult? = null,
    var fulfillments: List<Fulfillment> = emptyList(),
) {
    val selfLink: String?
        get() = links?.get("self")?.href

    val authorizePaymentLink: String?
        get() = links?.get("authorizePayment")?.href

    val originCandidateLink: String?
        get() = paymentResult?.originCandidateLink
}

data class Pricing(
    var price: Price? = null,
)