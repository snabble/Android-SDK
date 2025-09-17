package io.snabble.sdk.ui.checkout

import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.BaseFragment
import io.snabble.sdk.ui.R
import io.snabble.sdk.utils.Logger

class AuthenticationFragment : BaseFragment(R.layout.snabble_fragment_authentication) {
    override fun onActualViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActualViewCreated(view, savedInstanceState)

        val currentCheckout = Snabble.checkedInProject.value?.checkout

        // Setup your views here after inflation
        val webview = view.findViewById<WebView>(R.id.authentication_webview)
        webview.setupCookies()

        // Configure WebView settings
        webview.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false

            // Additional cookie-related settings
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webview.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                Logger.d("onReceivedError $failingUrl")
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                when (request.url.toString()) {
                    SUCCESS,
                    ERROR,
                    BACK -> {
                        // No need to pop it since we're polling for the checkout state in the activity
                        // and navigate away as soon the get an update state
                        return true
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        val redirectUrl = currentCheckout?.checkoutProcess?.paymentRedirect
        redirectUrl?.let {
            webview.loadUrl(it)
        }
    }

    // Extension function for cleaner code
    fun WebView.setupCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)
        cookieManager.flush()
    }

    private companion object {

        const val SUCCESS = "snabble://payone/success"
        const val ERROR = "snabble://payone/error"
        const val BACK = "snabble://payone/back"
    }
}
