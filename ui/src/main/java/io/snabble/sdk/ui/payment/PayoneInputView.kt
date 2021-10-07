package io.snabble.sdk.ui.payment

import android.widget.FrameLayout
import android.widget.ProgressBar
import io.snabble.sdk.Project
import android.widget.TextView
import android.annotation.SuppressLint
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.payment.Payone.PayoneTokenizationData
import androidx.lifecycle.Lifecycle
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.Keyguard
import io.snabble.sdk.payment.PaymentCredentials
import io.snabble.sdk.ui.telemetry.Telemetry
import io.snabble.sdk.ui.SnabbleUI
import android.app.Application.ActivityLifecycleCallbacks
import io.snabble.sdk.utils.SimpleActivityLifecycleCallbacks
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.util.AttributeSet
import android.webkit.*
import android.widget.Toast
import androidx.annotation.Keep
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import io.snabble.sdk.PaymentMethod
import io.snabble.sdk.ui.utils.UIUtils
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.Logger
import okhttp3.*
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.math.BigDecimal
import java.nio.charset.Charset
import java.text.NumberFormat
import java.util.*

class PayoneInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    private var acceptedKeyguard = false
    private lateinit var webView: WebView
    private var cancelPreAuthUrl: String? = null
    private lateinit var progressBar: ProgressBar
    private var isActivityResumed = false
    private var pendingCreditCardInfo: CreditCardInfo? = null
    private lateinit var paymentType: PaymentMethod
    private lateinit var projectId: String
    private lateinit var tokenizationData: PayoneTokenizationData
    private var lastProject: Project? = null
    private lateinit var threeDHint: TextView


    @SuppressLint("InlinedApi", "SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun inflateView() {
        checkActivityResumed()
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
            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON);
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
        this.projectId = projectId
        this.paymentType = paymentType
        this.tokenizationData = tokenizationData
        inflateView()
    }

    private fun checkActivityResumed() {
        val fragmentActivity = UIUtils.getHostFragmentActivity(context)
        isActivityResumed =
            fragmentActivity?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) ?: true
    }

    private fun loadForm() {
        try {
            val ccType = when(paymentType) {
                PaymentMethod.AMEX -> "A"
                PaymentMethod.MASTERCARD -> "M"
                PaymentMethod.VISA -> "V"
                else -> "V"
            }

            val htmlForm = IOUtils.toString(
                resources.openRawResource(R.raw.snabble_payoneform),
                Charset.forName("UTF-8")
            ).replace("{{hash}}", tokenizationData.hash)
            .replace("{{merchantID}}", tokenizationData.merchantID)
            .replace("{{portalID}}", tokenizationData.portalID)
            .replace("{{accountID}}", tokenizationData.accountID)
            .replace("{{mode}}", if(tokenizationData.isTesting) "test" else "live")
            .replace("{{language}}", Locale.getDefault().language)
            .replace("{{supportedCardType}}", ccType)
            // TODO: l10n
            .replace("{{lastName}}", "Nachname")
            .replace("{{cardNumberLabel}}", "Kartennummer")
            .replace("{{cvcLabel}}", "Prüfnummer (CVC)")
            .replace("{{expireMonthLabel}}", "Ablaufmonat (MM)")
            .replace("{{expireYearLabel}}", "Ablaufjahr (JJJJ)")
            .replace("{{saveButtonLabel}}", "Speichern")
            .replace("{{incompleteForm}}", "Bitte fülle das Formular vollständig aus.")
            // URL is required for an origin check
            webView.loadDataWithBaseURL(
                "https://snabble.io",
                htmlForm,
                null,
                "utf-8",
                "https://snabble.io"
            )
            val project = Snabble.getInstance().projects.first { it.id == projectId }
            var companyName = project.name
            if (project.company != null && project.company.getName() != null) {
                companyName = project.company.getName()
            }
            val numberFormat = NumberFormat.getCurrencyInstance(
                project.currencyLocale
            )
            val chargeTotal = BigDecimal(tokenizationData.chargeAmount?:"0")
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

    private fun authenticateAndSave(creditCardInfo: CreditCardInfo) {
        cancelPreAuth(creditCardInfo)
        Keyguard.unlock(UIUtils.getHostFragmentActivity(context), object : Keyguard.Callback {
            override fun success() {
                save(creditCardInfo)
            }

            override fun error() {
                if (isShown) {
                    finish()
                } else {
                    acceptedKeyguard = true
                }
            }
        })
    }

    private fun save(info: CreditCardInfo) {
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
            projectId
        )
        if (pc == null) {
            Toast.makeText(context, "Could not verify payment credentials", Toast.LENGTH_LONG)
                .show()
        } else {
            Snabble.getInstance().paymentCredentialsStore.add(pc)
            Telemetry.event(Telemetry.Event.PaymentMethodAdded, pc.type.name)
        }
        if (isShown) {
            finish()
        } else {
            acceptedKeyguard = true
        }
    }

    private fun cancelPreAuth(creditCardInfo: CreditCardInfo) {
        var url = cancelPreAuthUrl
        if (url == null) {
            Logger.e("Could not abort pre authorization, no url provided")
            return
        }
        if (lastProject == null) {
            Logger.e("Could not abort pre authorization, no project provided")
            return
        }
        // TODO wait for backend to implement pre auth
        /*
        url = url.replace("{orderID}", creditCardInfo.transactionId)
        val request: Request = Request.Builder()
            .url(Snabble.getInstance().absoluteUrl(url))
            .delete()
            .build()

        // fire and forget
        lastProject!!.okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // ignore
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                // ignore
            }
        })
         */
    }

    private fun finish() {
        val callback = SnabbleUI.getUiCallback()
        callback?.execute(SnabbleUI.Action.GO_BACK, null)
    }

    private fun finishWithError(failReason: String?) {
        var errorMessage = context.getString(R.string.Snabble_Payment_CreditCard_error)
        if (failReason != null) {
            errorMessage = "$errorMessage: $failReason"
        }
        Toast.makeText(
            context,
            errorMessage,
            Toast.LENGTH_SHORT
        )
            .show()
        finish()
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkActivityResumed()
        val application = context.applicationContext as Application
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        val application = context.applicationContext as Application
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private val activityLifecycleCallbacks: ActivityLifecycleCallbacks =
        object : SimpleActivityLifecycleCallbacks() {
            override fun onActivityStarted(activity: Activity) {
                if (UIUtils.getHostActivity(context) === activity) {
                    if (acceptedKeyguard) {
                        finish()
                        acceptedKeyguard = false
                    }
                }
            }

            override fun onActivityResumed(activity: Activity) {
                super.onActivityResumed(activity)
                isActivityResumed = true
                if (pendingCreditCardInfo != null) {
                    authenticateAndSave(pendingCreditCardInfo!!)
                    pendingCreditCardInfo = null
                }
            }

            override fun onActivityPaused(activity: Activity) {
                super.onActivityPaused(activity)
                isActivityResumed = false
            }
        }

    private data class CreditCardInfo(
        val pseudocardpan: String,
        val truncatedcardpan: String,
        val cardtype: String,
        val cardexpiredate: String,
        val lastname: String
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
            val creditCardInfo = CreditCardInfo(
                pseudocardpan = requireNotNull(pseudocardpan),
                truncatedcardpan = requireNotNull(truncatedcardpan),
                cardtype = requireNotNull(cardtype),
                cardexpiredate = requireNotNull(cardexpiredate),
                lastname = requireNotNull(lastname),
            )
            if (isActivityResumed) {
                Dispatch.mainThread { authenticateAndSave(creditCardInfo) }
            } else {
                pendingCreditCardInfo = creditCardInfo
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