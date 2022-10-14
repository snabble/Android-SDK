package io.snabble.sdk.checkout

import android.annotation.SuppressLint
import com.google.gson.JsonObject
import io.snabble.sdk.*
import io.snabble.sdk.ShoppingCart.BackendCart
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.utils.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DefaultCheckoutApi(private val project: Project,
                         private val shoppingCart: ShoppingCart) : CheckoutApi {
    private val okHttpClient: OkHttpClient = project.okHttpClient
    private var call: Call? = null

    override fun cancel() {
        call?.cancel()
        call = null
    }

    override fun abort(
        checkoutProcessResponse: CheckoutProcessResponse,
        paymentAbortResult: PaymentAbortResult?
    ) {
        if (checkoutProcessResponse.selfLink == null) {
            paymentAbortResult?.onError()
            return
        }

        val request = Request.Builder()
            .url(Snabble.absoluteUrl(checkoutProcessResponse.selfLink!!))
            .patch("{\"aborted\":true}".toRequestBody(JSON))
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Logger.d("Payment aborted")
                    paymentAbortResult?.onSuccess()
                } else {
                    Logger.e("Error while aborting payment")
                    paymentAbortResult?.onError()
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                Logger.e("Error while aborting payment")
                paymentAbortResult?.onError()
            }
        })
    }

    override fun createCheckoutInfo(
        backendCart: BackendCart,
        checkoutInfoResult: CheckoutInfoResult?,
        timeout: Long
    ) {
        val checkoutUrl = project.checkoutUrl
        if (checkoutUrl == null) {
            Logger.e("Could not checkout, no checkout url provided in metadata")
            checkoutInfoResult?.onConnectionError()
            return
        }

        val request = Request.Builder()
            .url(Snabble.absoluteUrl(checkoutUrl))
            .post(GsonHolder.get().toJson(backendCart).toRequestBody(JSON))
            .build()

        cancel()

        var okClient = okHttpClient
        if (timeout > 0) {
            okClient = okClient.newBuilder()
                .callTimeout(timeout, TimeUnit.MILLISECONDS)
                .build()
        }

        call = okClient.newCall(request)
        call?.enqueue(object : JsonCallback<SignedCheckoutInfo, JsonObject>(
            SignedCheckoutInfo::class.java, JsonObject::class.java) {
            override fun success(signedCheckoutInfo: SignedCheckoutInfo) {
                val price = signedCheckoutInfo.checkoutInfo
                    ?.get("price")
                    ?.asJsonObject
                    ?.get("price")
                    ?.asInt
                    ?: shoppingCart.totalPrice

                val availablePaymentMethods = signedCheckoutInfo.getAvailablePaymentMethods()
                if (availablePaymentMethods.isNotEmpty()) {
                    checkoutInfoResult?.onSuccess(signedCheckoutInfo, price, availablePaymentMethods)
                } else {
                    checkoutInfoResult?.onConnectionError()
                }
            }

            override fun failure(obj: JsonObject) {
                try {
                    val error = obj["error"].asJsonObject
                    val type = error["type"].asString
                    when (type) {
                        "invalid_cart_item" -> {
                            val invalidSkus: MutableList<String> = ArrayList()
                            val arr = error["details"].asJsonArray

                            arr.forEach {
                                val sku = it.asJsonObject["sku"].asString
                                invalidSkus.add(sku)
                            }

                            val invalidProducts = mutableListOf<Product>()
                            for (i in 0..shoppingCart.size()) {
                                val product = shoppingCart[i].product
                                if (product != null) {
                                    if (invalidSkus.contains(product.sku)) {
                                        invalidProducts.add(product)
                                    }
                                }
                            }
                            Logger.e("Invalid products")
                            checkoutInfoResult?.onInvalidProducts(invalidProducts)
                        }
                        "no_available_method" -> checkoutInfoResult?.onNoAvailablePaymentMethodFound()
                        "bad_shop_id", "shop_not_found" -> checkoutInfoResult?.onNoShopFound()
                        "invalid_deposit_return_voucher" -> checkoutInfoResult?.onInvalidDepositReturnVoucher()
                        else -> checkoutInfoResult?.onUnknownError()
                    }
                } catch (e: Exception) {
                    error(e)
                }
            }

            override fun error(t: Throwable) {
                Logger.e("Error creating checkout info: " + t.message)
                checkoutInfoResult?.onConnectionError()
            }
        })
    }

    private fun updatePaymentProcess(
        url: String,
        paymentProcessResult: PaymentProcessResult?
    ) {
        val request = Request.Builder()
            .url(Snabble.absoluteUrl(url))
            .get()
            .build()

        cancel()

        call = okHttpClient.newCall(request)
        call?.enqueue(object : SimpleJsonCallback<CheckoutProcessResponse>(CheckoutProcessResponse::class.java) {
            override fun success(checkoutProcessResponse: CheckoutProcessResponse) {
                paymentProcessResult?.onSuccess(checkoutProcessResponse, rawResponse())
                call = null
            }

            override fun error(t: Throwable) {
                if (responseCode() == 404) {
                    paymentProcessResult?.onNotFound()
                } else {
                    paymentProcessResult?.onError()
                }
            }
        })
    }

    override fun updatePaymentProcess(
        checkoutProcessResponse: CheckoutProcessResponse,
        paymentProcessResult: PaymentProcessResult?
    ) {
        val url = checkoutProcessResponse.selfLink
        if (url == null) {
            paymentProcessResult?.onError()
            return
        }
        updatePaymentProcess(url, paymentProcessResult)
    }

    @SuppressLint("SimpleDateFormat")
    override fun createPaymentProcess(
        id: String,
        signedCheckoutInfo: SignedCheckoutInfo,
        paymentMethod: PaymentMethod,
        processedOffline: Boolean,
        paymentCredentials: PaymentCredentials?,
        finalizedAt: Date?,
        paymentProcessResult: PaymentProcessResult?
    ) {
        if (paymentCredentials != null) {
            val data = paymentCredentials.encryptedData
            if (data == null) {
                paymentProcessResult?.onError()
                return
            }
        }

        val checkoutProcessRequest = CheckoutProcessRequest(
            paymentMethod = paymentMethod,
            signedCheckoutInfo = signedCheckoutInfo,
            processedOffline = processedOffline,
            finalizedAt = finalizedAt?.let { DateUtils.toRFC3339(it) },
            paymentInformation = when (paymentCredentials?.type) {
                PaymentCredentials.Type.CREDIT_CARD_PSD2 -> {
                    PaymentInformation(
                        originType = paymentCredentials.type.originType,
                        encryptedOrigin = paymentCredentials.encryptedData,
                        validUntil = SimpleDateFormat("yyyy/MM/dd").format(Date(paymentCredentials.validTo)),
                        cardNumber = paymentCredentials.obfuscatedId,
                    )
                }
                PaymentCredentials.Type.PAYDIREKT -> {
                    PaymentInformation(
                        originType = paymentCredentials.type.originType,
                        encryptedOrigin = paymentCredentials.encryptedData,
                        deviceID = paymentCredentials.additionalData["deviceID"],
                        deviceName = paymentCredentials.additionalData["deviceName"],
                        deviceFingerprint = paymentCredentials.additionalData["deviceFingerprint"],
                        deviceIPAddress = paymentCredentials.additionalData["deviceIPAddress"],
                    )
                }
                null -> null
                else -> {
                    PaymentInformation(
                        originType = paymentCredentials.type.originType,
                        encryptedOrigin = paymentCredentials.encryptedData
                    )
                }
            }
        )

        var url = signedCheckoutInfo.checkoutProcessLink
        if (url == null) {
            paymentProcessResult?.onError()
            return
        }
        url = "$url/$id"

        val json = GsonHolder.get().toJson(checkoutProcessRequest)
        val request = Request.Builder()
            .url(Snabble.absoluteUrl(url))
            .put(json.toRequestBody(JSON))
            .build()

        cancel()

        call = okHttpClient.newCall(request)
        call?.enqueue(object :
            SimpleJsonCallback<CheckoutProcessResponse>(CheckoutProcessResponse::class.java) {
            override fun success(checkoutProcess: CheckoutProcessResponse) {
                paymentProcessResult?.onSuccess(checkoutProcess, rawResponse())
            }

            override fun error(t: Throwable) {
                // Legacy: In case of a conflicting checkout process
                // the backend currently responds with a 403. This
                // will change in the future to the correct status 409
                if (responseCode() == 403 || responseCode() == 409) {
                    updatePaymentProcess(url, paymentProcessResult)
                } else if (responseCode() == 404) {
                    paymentProcessResult?.onNotFound()
                } else {
                    paymentProcessResult?.onError()
                }
            }
        })
    }

    override fun authorizePayment(
        checkoutProcessResponse: CheckoutProcessResponse,
        authorizePaymentRequest: AuthorizePaymentRequest,
        authorizePaymentResult: AuthorizePaymentResult?
    ) {
        val url = checkoutProcessResponse.authorizePaymentLink
        if (url == null) {
            authorizePaymentResult?.onError()
            return
        }

        val json = GsonHolder.get().toJson(authorizePaymentRequest)
        val request = Request.Builder()
            .url(Snabble.absoluteUrl(url))
            .post(json.toRequestBody(JSON))
            .build()

        val call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                authorizePaymentResult?.onError()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    authorizePaymentResult?.onSuccess()
                } else {
                    authorizePaymentResult?.onError()
                }
            }
        })
    }

    companion object {
        private val JSON: MediaType = "application/json".toMediaType()
    }
}
