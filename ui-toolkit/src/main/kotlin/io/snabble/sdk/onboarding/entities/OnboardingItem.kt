package io.snabble.sdk.onboarding.entities

import android.os.Parcelable
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
    val imageSource: String?,
    val title: String?,
    val text: String?,
    val footer: String?,
    val nextButtonTitle: String?,
    val prevButtonTitle: String?,
    val termsButtonTitle: String?,
    val link: String?
) : Parcelable