package io.snabble.sdk.onboarding.terms

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import io.snabble.sdk.ui.toolkit.R
import io.snabble.sdk.utils.getColorByAttribute
import okhttp3.internal.toHexString


/**
 * Displays the html string in web view
 */

abstract class RawHtmlFragment : Fragment() {
    abstract val html: String
    abstract val header: String
    private lateinit var webView: WebView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.snabble_fragment_raw_html, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.web_view)
        webView.setBackgroundColor(requireContext().getColorByAttribute(R.attr.colorSurface))
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return shouldOverrideUrlLoading(request.url)
            }
        }

        showHtml(html)
    }

    open fun shouldOverrideUrlLoading(url: Uri): Boolean {
        try {
            openUrl(url.toString())
        } catch (e: Exception) {}
        return true
    }

    private fun showHtml(html: String) {
        val media = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> "* { color: #fff }".trimIndent()
            else -> ""
        }

        val attrs = intArrayOf(R.attr.colorPrimary)
        val arr = requireContext().obtainStyledAttributes(attrs)
        val primaryColor = arr.getColor(0, -1)
        arr.recycle()

        val embed = """
            <html>
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <style type="text/css">
                body { padding: 8px 8px }
                * { font-family: Roboto, sans-serif; font-size: 15px; word-wrap: break-word; color: #000 }
                h1 { font-size: 22px }
                h2 { font-size: 17px }
                h4 { font-weight: normal; color: #3c3c43; opacity: 0.6 }
                $media
                a { color: #${(primaryColor and 0xffffff).toHexString()} }
                </style>
              </head>
              <body>
                $header
                $html
              </body>
            </html>
        """

        webView.loadDataWithBaseURL(null, embed, "text/html", "UTF-8", null)
    }

    fun Fragment.openUrl(url: String) = startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) })
}