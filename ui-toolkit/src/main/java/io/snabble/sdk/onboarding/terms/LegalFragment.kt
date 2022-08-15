package io.snabble.sdk.onboarding.terms

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController

class LegalFragment : RawHtmlFragment() {

    override val header by lazy {
        val headerImage = arguments?.getString(headerImagePath)?.let { imagePath ->
            """<img src="file://$imagePath" 
                     style="display: block; margin-left: auto; margin-right: auto; height: auto; max-width: 75%;"/>"""
        }.orEmpty()

        val headerTitle = arguments?.getInt(headerTitle)?.let { headerTitleResId ->
            if (headerTitleResId != Resources.ID_NULL){
            """<p style="text-align: center;">${resources.getText(headerTitleResId)}</p>"""}
            else ""
        }.orEmpty()

        headerImage + headerTitle
    }

    override val html by lazy {
        resources.openRawResource(requireNotNull(arguments?.getInt(resId))).bufferedReader()
            .readText()
    }

    override fun shouldOverrideUrlLoading(url: Uri): Boolean {
        val currentDeeplink = (arguments?.get(NavController.KEY_DEEP_LINK_INTENT) as? Intent)?.data

        // Check if this our scheme and we can navigate directly internal
        if (url.scheme == currentDeeplink?.scheme) {
            // Parse current query params to the requested Deeplink e.g. for hiding bottom navbar
            val target = url.buildUpon().encodedQuery(currentDeeplink?.encodedQuery).build()
            findNavController().navigate(NavDeepLinkRequest.Builder.fromUri(target).build())
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