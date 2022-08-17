package io.snabble.sdk.onboarding.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Global configuration for the onboarding layout
 * imageSource: Set the Logo on top of each page
 * hasPageControl: enables circle indicator and swipe function. If set to false button only navigation is enabled
 */
@Parcelize
data class OnboardingConfiguration (
    val imageSource: String?,
    val hasPageControl: Boolean?
) : Parcelable