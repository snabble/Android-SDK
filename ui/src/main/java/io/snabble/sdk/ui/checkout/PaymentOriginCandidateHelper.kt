package io.snabble.sdk.ui.checkout

import androidx.annotation.RestrictTo
import com.google.gson.JsonObject
import io.snabble.sdk.Project
import io.snabble.sdk.checkout.CheckoutProcessResponse
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.SimpleJsonCallback
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.checkout.Href
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.Serializable
import java.util.concurrent.CopyOnWriteArrayList

@RestrictTo(RestrictTo.Scope.LIBRARY)
class PaymentOriginCandidateHelper(val project: Project) {
    private val listeners: MutableList<PaymentOriginCandidateAvailableListener> = CopyOnWriteArrayList()
    private var isPolling = false
    private var call: Call? = null

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun startPollingIfLinkIsAvailable(checkoutProcessResponse: CheckoutProcessResponse?) {
        if (checkoutProcessResponse?.originCandidateLink == null) {
            return
        }
        if (isPolling) {
            return
        }
        isPolling = true
        poll(checkoutProcessResponse)
    }

    private fun poll(checkoutProcessResponse: CheckoutProcessResponse) {
        if (!isPolling) {
            return
        }

        Dispatch.background({
            val request: Request = Request.Builder()
                .get()
                .url(Snabble.absoluteUrl(checkoutProcessResponse.originCandidateLink!!))
                .build()

            call = project.okHttpClient.newCall(request)
            call?.enqueue(object : SimpleJsonCallback<PaymentOriginCandidate>(PaymentOriginCandidate::class.java) {
                override fun success(paymentOriginCandidate: PaymentOriginCandidate) {
                    if (paymentOriginCandidate.isValid) {
                        paymentOriginCandidate.projectId = project.id
                        notifyPaymentOriginCandidateAvailable(paymentOriginCandidate)
                        stopPolling()
                    } else {
                        poll(checkoutProcessResponse)
                    }
                }

                override fun error(t: Throwable) {
                    if (responseCode() >= 500) {
                        poll(checkoutProcessResponse)
                    }
                }
            })
        }, 1000)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun stopPolling() {
        call?.cancel()
        isPolling = false
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun addPaymentOriginCandidateAvailableListener(listener: PaymentOriginCandidateAvailableListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal fun removePaymentOriginCandidateAvailableListener(listener: PaymentOriginCandidateAvailableListener) {
        listeners.remove(listener)
    }

    private fun notifyPaymentOriginCandidateAvailable(paymentOriginCandidate: PaymentOriginCandidate) {
        Dispatch.mainThread {
            for (listener in listeners) {
                listener.onPaymentOriginCandidateAvailable(paymentOriginCandidate)
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class PaymentOriginCandidate : Serializable {
        var projectId: String? = null
        @JvmField
        var origin: String? = null
        var links: Map<String, Href>? = null
        fun promote(paymentCredentials: PaymentCredentials, result: PromoteResult) {
            val promoteLink = promoteLink
            if (promoteLink == null) {
                result.error()
                return
            }

            val jsonObject = JsonObject()
            jsonObject.addProperty("origin", paymentCredentials.encryptedData)

            val request: Request = Request.Builder()
                .post(GsonHolder.get().toJson(jsonObject).toRequestBody("application/json".toMediaType()))
                .url(Snabble.absoluteUrl(promoteLink))
                .build()

            val project = Snabble.getProjectById(projectId)
            project?.okHttpClient?.newCall(request)?.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    result.error()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.code == 201) {
                        result.success()
                    } else {
                        result.error()
                    }
                }
            })
        }

        private val promoteLink: String?
            get() {
                val link = links!!["promote"]
                return if (link != null && link.href != null) {
                    link.href
                } else null
            }

        val isValid: Boolean
            get() = origin != null && promoteLink != null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface PromoteResult {
        fun success()
        fun error()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface PaymentOriginCandidateAvailableListener {
        fun onPaymentOriginCandidateAvailable(paymentOriginCandidate: PaymentOriginCandidate?)
    }
}