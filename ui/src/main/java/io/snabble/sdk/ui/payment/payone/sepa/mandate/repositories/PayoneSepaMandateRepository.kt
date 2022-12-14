package io.snabble.sdk.ui.payment.payone.sepa.mandate.repositories

import io.snabble.sdk.BuildConfig
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.checkout.CheckoutProcessResponse
import io.snabble.sdk.utils.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

internal interface PayoneSepaMandateRepository {

    fun createSepaMandateHtml(): String?

    suspend fun acceptMandate(): Boolean

    fun abortProcess()
}

internal class PayoneSepaMandateRepositoryImpl(
    private val snabble: Snabble,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PayoneSepaMandateRepository {

    override fun createSepaMandateHtml(): String? {
        val mandate = snabble.checkedInProject.value
            ?.checkout
            ?.checkoutProcess
            ?.paymentPreauthInformation
            ?.get(JSON_KEY_MANDATE)
            ?.asString
            ?.decode() ?: return null

        return createHtml(mandate)
    }

    override suspend fun acceptMandate(): Boolean = withContext(ioDispatcher) {
        val project: Project = snabble.checkedInProject.value ?: return@withContext false
        val checkoutProcess: CheckoutProcessResponse = project.checkout.checkoutProcess ?: return@withContext false
        val mandateId: String = checkoutProcess.paymentPreauthInformation
            ?.get(JSON_KEY_MANDATE_ID)
            ?.asString ?: return@withContext false
        val url: String = checkoutProcess.links
            ?.get(LINK_MAP_AUTH_PAYMENT_KEY)
            ?.href
            ?.let { Snabble.absoluteUrl(it) } ?: return@withContext false

        val contentType = CONTENT_TYPE_JSON.toMediaType()
        val request: Request = Request.Builder()
            .url(url = url)
            .post("{ \"mandateReference\": \"$mandateId\" }".toRequestBody(contentType))
            .build()

        if (BuildConfig.DEBUG) delay(2.seconds)
        suspendCancellableCoroutine { continuation ->
            val call: Call = project.okHttpClient.newCall(request)
            call.enqueue(object : Callback {

                override fun onResponse(call: Call, response: Response) {
                    Logger.d(
                        "PAYONE SEPA mandate accept request successful. " +
                                "HTTP-STATUS<${response.code}>: ${response.body?.string()}"
                    )
                    if (isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    Logger.d("PAYONE SEPA mandate accept request failed: $e")
                    if (isActive) {
                        continuation.resume(false)
                    }
                }
            })

            continuation.invokeOnCancellation {
                call.cancel()
            }
        }
    }

    override fun abortProcess() {
        snabble.checkedInProject.value?.checkout?.abort()
    }

    private companion object {

        const val JSON_KEY_MANDATE = "markup"
        const val JSON_KEY_MANDATE_ID = "mandateIdentification"

        const val LINK_MAP_AUTH_PAYMENT_KEY = "authorizePayment"

        const val CONTENT_TYPE_JSON = "application/json"
    }
}

private fun String.decode(encoding: String = "UTF-8"): String? = try {
    URLDecoder.decode(this, encoding)
} catch (ignored: UnsupportedEncodingException) {
    null
}

private fun createHtml(body: String): String = HEAD + body + TAIL

private const val HEAD = """
<!DOCTYPE html>
<html>
    <head>
        <title>&nbsp;</title>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
    </head>
"""

private const val TAIL = """
    </body>
</html>
"""
