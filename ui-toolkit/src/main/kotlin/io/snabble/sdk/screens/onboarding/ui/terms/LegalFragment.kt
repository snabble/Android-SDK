package io.snabble.sdk.screens.onboarding.ui.terms

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Base64
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.utils.getImageId
import java.io.ByteArrayOutputStream

class LegalFragment : RawHtmlFragment() {

    /**
     * If set in the navArgs:
     * converts the path of the header image and the title for the header into a readable Html block
     */
    override val header by lazy {
        val rawPath = arguments?.getString(headerImagePath)
        val url = when {
            rawPath?.startsWith("android_asset/") == true -> "file://$rawPath"
            rawPath?.startsWith("android_res/drawable/") == true -> buildImageUrl(rawPath)
            else -> null
        }
        val headerImage = url?.let {
            """<img src="$url" style="display:block; margin:0 auto; height:auto; max-width:75%;"/>"""
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
        SnabbleUiToolkit.executeAction(
            requireContext(),
            SnabbleUiToolkit.Event.SHOW_ONBOARDING_DONE,
            bundleOf(SnabbleUiToolkit.DEEPLINK to url)
        )

        return true
    }

    // check if drawable exists and if it is a vector drawable convert it to a data url for the webview
    private fun buildImageUrl(rawPath: String) : String? {
        val ctx = requireContext()
        val resId = ctx.getImageId(rawPath.substringAfterLast('/'))
        val img = AppCompatResources.getDrawable(ctx, resId) ?: return null
        val imgClass = img.javaClass.name.orEmpty()
        return if (imgClass.startsWith("androidx.") || imgClass.contains("Vector")) {
            val bitmap = Bitmap.createBitmap(img.intrinsicWidth, img.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            img.setBounds(0, 0, canvas.width, canvas.height)
            img.draw(canvas)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream)
            val encodedImage = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            bitmap.recycle()
            "data:image/png;base64,$encodedImage"
        } else "file://$rawPath"
    }

    companion object {
        private const val resId = "resId"
        private const val headerImagePath = "imagePath"
        private const val headerTitle = "headerTitle"
    }
}