package io.snabble.sdk.screens.onboarding.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * configuration for each page in the onboarding layout
 *
 * Text body:
 * title, text and footer display the text body. Each comes with a different size
 *
 * Button:
 * 1. if only one button is set the button is set to fullscreen
 * 2. set up prevButtonTitle for backward navigation
 * 3. set up nextButtonTitle for forward navigation
 * 4. if page control is disabled set up prev and next button for backward and forward navigation
 *
 * Terms button and link:
 * Always set up both since the link is needed to navigate from the terms button
 */
@Parcelize
data class OnboardingItem(
    @SerializedName ("imageSource") val imageSource: String?,
    @SerializedName ("title")val title: String?,
    @SerializedName ("text")val text: String?,
    @SerializedName ("footer")val footer: String?,
    @SerializedName ("customButtonTitle")val customButtonTitle: String?,
    @SerializedName ("termsButtonTitle")val termsButtonTitle: String?,
    @SerializedName ("link")val link: String?
) : Parcelable
