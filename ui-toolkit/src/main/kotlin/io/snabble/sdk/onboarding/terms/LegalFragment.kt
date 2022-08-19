package io.snabble.sdk.onboarding.terms

import android.content.res.Resources
import android.net.Uri
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import io.snabble.sdk.utils.matchesAppScheme
import io.snabble.sdk.utils.appendDeeplinkQueryParams

class LegalFragment : RawHtmlFragment() {

    /**
     * If set in the navArgs:
     * converts the path of the header image and the title for the header into a readable Html block
     */
    override val header by lazy {
        // TODO check if that asset exists, if not and this is a resource (starts with android_res), it should be checked if this is a vector drawable.
        // If so you need to convert it to a data url first
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

    /**
     * If set in the navArgs:
     * loads the Html file from the raw folder
     */
    override val html by lazy {
        resources.openRawResource(requireNotNull(arguments?.getInt(resId))).bufferedReader()
            .readText()
    }

    /**
     * compares the given Url. If it matches with the current app scheme it appends the deeplink query params to it.
     * Otherwise it opens the url directly.
     */
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