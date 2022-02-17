package io.snabble.sdk.ui.payment

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.util.AttributeSet
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import eu.rekisoft.android.util.LazyWorker
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.Project
import io.snabble.sdk.Snabble
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.ui.Keyguard
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.SnabbleUI
import io.snabble.sdk.ui.payment.Payone.PayoneTokenizationData
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.GsonHolder
import io.snabble.sdk.utils.Logger
import io.snabble.sdk.utils.SimpleJsonCallback
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.math.BigDecimal
import java.nio.charset.Charset
import java.text.NumberFormat
import java.util.*
import java.util.concurrent.CancellationException

class PayoneInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr), LifecycleObserver {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var isActivityResumed = false
    private var creditCardInfo: CreditCardInfo? = null
    private lateinit var paymentType: PaymentMethod
    private lateinit var project: Project
    private lateinit var tokenizationData: PayoneTokenizationData
    private lateinit var threeDHint: TextView
    private var lastPreAuthResponse: Payone.PreAuthResponse? = null
    private var polling = LazyWorker.createLifeCycleAwareJob(context) {
        lastPreAuthResponse?.links?.get("preAuthStatus")?.href?.let { statusUrl ->
            val request = Request.Builder()
                .url(Snabble.absoluteUrl(Snabble.absoluteUrl(statusUrl)))
                .build()
            project.okHttpClient.newCall(request).enqueue(object :
                SimpleJsonCallback<Payone.PreAuthResponse>(Payone.PreAuthResponse::class.java),
                Callback {
                override fun success(response: Payone.PreAuthResponse?) {
                    response?.let {
                        when (it.status) {
                            Payone.AuthStatus.pending -> doLater(1000)
                            Payone.AuthStatus.successful -> {
                                creditCardInfo?.let { cardInfo ->
                                    cardInfo.userId = it.userID
                                    authenticateAndSave(cardInfo)
                                } ?: finishWithError()
                            }
                            Payone.AuthStatus.failed -> finishWithError()
                        }
                    } ?: error(null)
                }

                override fun error(t: Throwable?) {
                    t?.let {
                        IllegalStateException("Error while saving card", it).printStackTrace()
                    }
                    if (t !is CancellationException) doLater(1000)
                }
            })
        }
    }

    @SuppressLint("InlinedApi", "SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun inflateView() {
        inflate(context, R.layout.snabble_view_cardinput_creditcard, this)
        progressBar = findViewById(R.id.progress)
        progressBar.visibility = VISIBLE
        progressBar.isIndeterminate = true
        webView = findViewById(R.id.web_view)
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                Dispatch.mainThread { finishWithError(null) }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                Dispatch.mainThread {
                    progressBar.isVisible = newProgress != 100
                    progressBar.isIndeterminate = false
                    progressBar.progress = newProgress
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.addJavascriptInterface(JsInterface(), "snabble")

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                Logger.e("onReceivedError $failingUrl")
                Dispatch.mainThread { finishWithError() }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val uri = request.url
                if (uri != null) {
                    val url = uri.toString()
                    Logger.d("shouldOverrideUrlLoading $url")
                    lastPreAuthResponse?.links?.let { links ->
                        when (url) {
                            links["redirectBack"]?.href -> finish()
                            links["redirectError"]?.href -> finishWithError()
                            links["redirectSuccess"]?.href -> polling.doNow()
                            else -> return false
                        }
                        return true
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }


        // this disables credit card storage prompt for google pay
        ViewCompat.setImportantForAutofill(webView, IMPORTANT_FOR_AUTOFILL_NO)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        threeDHint = findViewById(R.id.threed_secure_hint)
        threeDHint.visibility = GONE
        loadForm()

        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            // Force dark mode when in dark mode
            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
        }
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    fun load(
        projectId: String,
        paymentType: PaymentMethod,
        tokenizationData: PayoneTokenizationData
    ) {
        this.project = Snabble.projects.first { it.id == projectId }
        this.paymentType = paymentType
        this.tokenizationData = tokenizationData
        inflateView()
    }

    private fun <T> T.anyOfTheseOfFirst(fallback: T, vararg options: T) =
        if (options.contains(this)) this else fallback

    private fun loadForm() {
        try {
            val ccType = when (paymentType) {
                PaymentMethod.AMEX -> "A"
                PaymentMethod.MASTERCARD -> "M"
                PaymentMethod.VISA -> "V"
                else -> "V"
            }

            val language = Locale.getDefault().language.anyOfTheseOfFirst("en", "de", "fr", "it", "es", "pt", "nl")

            val htmlForm = IOUtils.toString(
                resources.openRawResource(R.raw.snabble_payoneform),
                Charset.forName("UTF-8")
            ).replace("{{hash}}", tokenizationData.hash)
            .replace("{{merchantID}}", tokenizationData.merchantID)
            .replace("{{portalID}}", tokenizationData.portalID)
            .replace("{{accountID}}", tokenizationData.accountID)
            .replace("{{mode}}", if(tokenizationData.isTesting) "test" else "live")
            .replace("{{language}}", language)
            .replace("{{supportedCardType}}", ccType)
            .replace("{{lastname}}", context.getString(R.string.Snabble_Payone_Lastname))
            .replace("{{cardNumberLabel}}", context.getString(R.string.Snabble_Payone_cardNumber))
            .replace("{{cvcLabel}}", context.getString(R.string.Snabble_Payone_cvc))
            .replace("{{expireMonthLabel}}", context.getString(R.string.Snabble_Payone_expireMonth))
            .replace("{{expireYearLabel}}", context.getString(R.string.Snabble_Payone_expireYear))
            .replace("{{saveButtonLabel}}", context.getString(R.string.Snabble_Save))
            .replace("{{incompleteForm}}", context.getString(R.string.Snabble_Payone_incompleteForm))
            // URL is required for an origin check
            webView.loadDataWithBaseURL(
                "https://snabble.io",
                htmlForm,
                null,
                "utf-8",
                "https://snabble.io"
            )
            var companyName = project.name
            if (project.company != null && project.company.getName() != null) {
                companyName = project.company.getName()
            }
            val numberFormat = NumberFormat.getCurrencyInstance(
                project.currencyLocale
            )
            val chargeTotal = BigDecimal(tokenizationData.preAuthInfo.amount ?: 0).divide(BigDecimal(100)) // TODO check currency
            threeDHint.visibility = VISIBLE
            threeDHint.text = resources.getString(
                R.string.Snabble_CC_3dsecureHint_retailerWithPrice,
                numberFormat.format(chargeTotal),
                companyName
            )
        } catch (e: IOException) {
            Logger.e(e.message)
            threeDHint.visibility = GONE
        }
    }

    fun Any.toJsonRequest(): RequestBody =
        GsonHolder.get().toJson(this).toRequestBody("application/json".toMediaTypeOrNull())

    private fun authenticate(creditCardInfo: CreditCardInfo) {
        val req = Payone.PreAuthRequest(creditCardInfo.pseudocardpan, creditCardInfo.lastname)
        val link = tokenizationData.links["preAuth"]
        if (link?.href != null) {
            val request = Request.Builder()
                .url(Snabble.absoluteUrl(Snabble.absoluteUrl(link.href)))
                .post(req.toJsonRequest())
                .build()

            project.okHttpClient.newCall(request).enqueue(object :
                SimpleJsonCallback<Payone.PreAuthResponse>(Payone.PreAuthResponse::class.java),
                Callback {
                override fun success(response: Payone.PreAuthResponse?) {
                    response?.let {
                        lastPreAuthResponse = it
                        response.links["scaChallenge"]?.href?.let { url ->
                            Dispatch.mainThread {
                                threeDHint.isVisible = false
                                webView.loadUrl(url)
                            }
                        } ?: error(null)
                    } ?: error(null)
                }

                override fun error(t: Throwable?) {
                    t?.printStackTrace()
                    Dispatch.mainThread { finishWithError() }
                }
            })
        } else {
            Dispatch.mainThread { finishWithError() }
        }
    }

    private fun authenticateAndSave(creditCardInfo: CreditCardInfo) {
        Keyguard.unlock(UIUtils.getHostFragmentActivity(context), object : Keyguard.Callback {
            override fun success() {
                save(creditCardInfo)
            }

            override fun error() {
                this@PayoneInputView.creditCardInfo = null
                finish()
            }
        })
    }

    private fun save(info: CreditCardInfo) {
        creditCardInfo = null
        val ccBrand = when (info.cardtype) {
            "V" -> PaymentCredentials.Brand.VISA
            "M" -> PaymentCredentials.Brand.MASTERCARD
            "A" -> PaymentCredentials.Brand.AMEX
            else -> PaymentCredentials.Brand.UNKNOWN
        }
        val pc = PaymentCredentials.fromPayone(
            info.pseudocardpan,
            info.truncatedcardpan,
            ccBrand,
            info.cardexpiredate,
            info.lastname,
            info.userId,
            project.id
        )
        if (pc == null) {
            Toast.makeText(context, "Could not verify payment credentials", Toast.LENGTH_LONG)
                .show()
        } else {
            Snabble.paymentCredentialsStore.add(pc)
            Telemetry.event(Telemetry.Event.PaymentMethodAdded, pc.type.name)
        }
        finish()
    }

    private fun finish() {
        SnabbleUI.executeAction(context, SnabbleUI.Event.GO_BACK)
    }

    private fun finishWithError(failReason: String? = null) {
        var errorMessage = context.getString(R.string.Snabble_Payment_CreditCard_error)
        if (failReason != null) {
            errorMessage = "$errorMessage: $failReason"
        }
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        finish()
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isActivityResumed = UIUtils.getHostFragmentActivity(context)?.lifecycle?.let { lifecycle ->
            lifecycle.addObserver(this)
            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        } ?: true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        isActivityResumed = true
        if (creditCardInfo != null) polling.doNow()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        isActivityResumed = false
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        UIUtils.getHostFragmentActivity(context)?.lifecycle?.removeObserver(this)
    }

    private data class CreditCardInfo(
        val pseudocardpan: String,
        val truncatedcardpan: String,
        val cardtype: String,
        val cardexpiredate: String,
        val lastname: String,
        var userId: String? = null,
    )

    @Keep
    private inner class JsInterface {
        @JavascriptInterface
        fun saveCard(
            pseudocardpan: String?,
            truncatedcardpan: String?,
            cardtype: String?,
            cardexpiredate: String?,
            lastname: String?
        ) {
            CreditCardInfo(
                pseudocardpan = requireNotNull(pseudocardpan),
                truncatedcardpan = requireNotNull(truncatedcardpan),
                cardtype = requireNotNull(cardtype),
                cardexpiredate = requireNotNull(cardexpiredate),
                lastname = requireNotNull(lastname),
            ).let { cardInfo ->
                creditCardInfo = cardInfo
                if (isActivityResumed) {
                    Dispatch.mainThread { authenticate(cardInfo) }
                }
            }
        }

        @JavascriptInterface
        fun fail(failReason: String?) {
            Dispatch.mainThread { finishWithError(failReason) }
        }

        @JavascriptInterface
        fun abort() {
            Dispatch.mainThread { finish() }
        }

        @JavascriptInterface
        fun toast(message: String?) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun log(message: String?) {
            Logger.d(message)
        }
    }

    companion object {
        const val ARG_PROJECT_ID = "projectId"
        const val ARG_PAYMENT_TYPE = "paymentType"
        const val ARG_TOKEN_DATA = "tokenData"
    }
}