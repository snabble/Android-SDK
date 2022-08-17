package io.snabble.sdk.onboarding.terms

import android.content.res.Resources
import android.net.Uri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import io.snabble.sdk.utils.matchesAppScheme
import io.snabble.sdk.utils.appendDeeplinkQueryParams

class LegalFragment : RawHtmlFragment() {

    override val header by lazy {
        val headerImage = arguments?.getString(headerImagePath)?.let { imagePath ->
            """<img src="file://$imagePath" 
                     style="display: block; margin-left: auto; margin-right: auto; height: auto; max-width: 75%;"/>"""
        }.orEmpty()

        val headerTitle = arguments?.getInt(headerTitle)?.let { headerTitleResId ->
            if (headerTitleResId != Resources.ID_NULL) {
            """<p style="text-align: center;">${resources.getText(headerTitleResId)}</p>"""
            } else null
        }.orEmpty()

        headerImage + headerTitle
    }

    override val html by lazy {
        resources.openRawResource(requireNotNull(arguments?.getInt(resId))).bufferedReader()
            .readText()
    }

    override fun shouldOverrideUrlLoading(url: Uri): Boolean {
        if (url.matchesAppScheme(arguments)) {
            findNavController().navigate(NavDeepLinkRequest.Builder.fromUri(url.appendDeeplinkQueryParams(arguments)).build())
        } else {
            try {
                openUrl(url.toString())
            } catch (e: Exception) {
            }
        }

        return true
    }

    companion object {
        private const val resId = "resId"
        private const val headerImagePath = "imagePath"
        private const val headerTitle = "headerTitle"
    }
}