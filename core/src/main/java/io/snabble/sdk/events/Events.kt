package io.snabble.sdk.events

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.snabble.sdk.Project
import io.snabble.sdk.Shop
import io.snabble.sdk.ShoppingCart
import io.snabble.sdk.ShoppingCart.SimpleShoppingCartListener
import io.snabble.sdk.ShoppingCart.Taxation
import io.snabble.sdk.Snabble
import io.snabble.sdk.codes.ScannedCode
import io.snabble.sdk.events.data.Event
import io.snabble.sdk.events.data.EventType
import io.snabble.sdk.events.data.payload.Payload
import io.snabble.sdk.events.data.payload.PayloadAnalytics
import io.snabble.sdk.events.data.payload.PayloadError
import io.snabble.sdk.events.data.payload.PayloadLog
import io.snabble.sdk.events.data.payload.PayloadProductNotFound
import io.snabble.sdk.events.data.payload.PayloadSessionEnd
import io.snabble.sdk.events.data.payload.PayloadSessionStart
import io.snabble.sdk.utils.DateUtils
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import io.snabble.sdk.utils.Utils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.Date
import java.util.IllegalFormatException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Class for dispatching events to the snabble Backend
 */
class Events @SuppressLint("SimpleDateFormat") internal constructor(
    private val project: Project,
    private val shoppingCart: ShoppingCart
) {

    private var cartId: String? = null
    private var shop: Shop? = null
    private val handler = Handler(Looper.getMainLooper())
    private var hasSentSessionStart = false

    init {
        project.shoppingCart.addListener(object : SimpleShoppingCartListener() {
            override fun onChanged(cart: ShoppingCart) {
                updateShop(Snabble.checkedInShop)
                shop?.let {
                    if (!hasSentSessionStart) {
                        val payloadSessionStart = PayloadSessionStart(session = cartId)
                        post(payloadSessionStart, false)
                    }
                    post(project.shoppingCart.toBackendCart(), true)
                }
            }

            override fun onCleared(cart: ShoppingCart) {
                val isSameCartWithNewId = shoppingCart === cart && cart.id != cartId
                if (isSameCartWithNewId) {
                    val payloadSessionEnd = PayloadSessionEnd(session = cartId)
                    post(payloadSessionEnd, false)
                    cartId = cart.id
                    hasSentSessionStart = false
                }
            }

            override fun onProductsUpdated(list: ShoppingCart) {
                // Override because it shouldn't trigger onChanged(Cart)
            }

            override fun onPricesUpdated(list: ShoppingCart) {
                // Override because it shouldn't trigger onChanged(Cart)
            }

            override fun onTaxationChanged(list: ShoppingCart, taxation: Taxation) {
                // Override because it shouldn't trigger onChanged(Cart)
            }

            override fun onCartDataChanged(list: ShoppingCart) {
                // Override because it shouldn't trigger onChanged(Cart)
            }
        })
    }

    /**
     * Updates the current shop which will be associated with future events
     */
    fun updateShop(newShop: Shop?) {
        if (newShop != null) {
            cartId = shoppingCart.id
            shop = newShop
        } else {
            shop = null
            cartId = null
            hasSentSessionStart = false
        }
    }

    /**
     * Pack a error log message into a event and dispatch it to the backend
     */
    fun logError(format: String?, vararg args: Any?) {
        if (Utils.isDebugBuild(Snabble.application)) {
            return  // do not log errors in debug builds
        }

        val error = PayloadError(message = getMessage(format, args), session = cartId)
        post(error, false)
    }

    private fun getMessage(format: String?, args: Array<out Any?>) =
        format?.let {
            try {
                String.format(it, args)
            } catch (e: IllegalFormatException) {
                Logger.e("Could not post event error: invalid format")
                null
            }
        }

    /**
     * Pack a log message into a event and dispatch it to the backend
     */
    fun log(format: String?, vararg args: Any?) {
        val log = PayloadLog(message = getMessage(format, args), session = cartId)
        post(log, false)
    }

    /**
     * Pack a analytics event and dispatch it to the backend
     */
    fun analytics(key: String?, value: String?, comment: String?) {
        val analytics = PayloadAnalytics(key = key, value = value, comment = comment, session = cartId)
        post(analytics, false)
    }

    /**
     * Dispatch a product not found event
     */
    fun productNotFound(scannedCodes: List<ScannedCode>?) {
        try {
            if (!scannedCodes.isNullOrEmpty()) {
                val payload = PayloadProductNotFound(
                    scannedCode = scannedCodes[0].code,
                    matched = scannedCodes.associate { it.templateName to it.lookupCode }
                )
                post(payload, false)
            } else {
                post(PayloadProductNotFound(), false)
            }
        } catch (e: ClassCastException) {
            Logger.e("Could not post event productNotFound: $e")
        }
    }

    private fun <T : Payload?> post(payload: T, debounce: Boolean) {
        val url = project.eventsUrl
        if (url == null) {
            Logger.e("Could not post event: no events url")
            return
        }

        val event = createEvent(payload)

        val requestBody = GsonHolder.get().toJson(event).toRequestBody("application/json".toMediaType())
        val request: Request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        if (debounce) {
            handler.removeCallbacksAndMessages(event.type)
            handler.postAtTime(
                { send(request, payload) },
                event.type,
                SystemClock.uptimeMillis() + 2000.milliseconds.inWholeMilliseconds
            )
        } else {
            send(request, payload)
        }
        if (event.type == EventType.SESSION_START) {
            hasSentSessionStart = true
        }
    }

    private fun <T : Payload?> createEvent(payload: T) = Event(
        type = payload!!.eventType,
        appId = Snabble.clientId,
        project = project.id,
        timestamp = DateUtils.toRFC3339(Date()),
        payload = GsonHolder.get().toJsonTree(payload),
        shopId = shop?.id
    )

    private fun <T : Payload?> send(request: Request, payload: T) {
        val okHttpClient = project.okHttpClient
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Logger.d("Successfully posted event: " + payload!!.eventType)
                } else {
                    Logger.e("Failed to post event: " + payload!!.eventType + ", code " + response.code)
                }
                response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                Logger.e("Could not post event: $e")
                if (payload!!.eventType == EventType.SESSION_START) {
                    hasSentSessionStart = false
                }
            }
        })
    }

    companion object {

        private fun getUsableProject(projectId: String?): Project? {
            // since we have no error logging without a project, we try to find the project by id
            // and if no project is found we just use the first project to at least log it to something
            var project = Snabble.getProjectById(projectId)
            if (project == null) {
                val projects = Snabble.projects
                if (projects.isNotEmpty()) {
                    project = projects[0]
                }
            }
            return project
        }

        /**
         * Log a error event to the project with the matching id
         */
        fun logErrorEvent(projectId: String?, format: String?, vararg args: Any?) {
            val project = getUsableProject(projectId)
            if (format == null) {
                project?.logErrorEvent("Broken log message")
            } else {
                project?.logErrorEvent(format, *args)
            }
        }
    }
}
