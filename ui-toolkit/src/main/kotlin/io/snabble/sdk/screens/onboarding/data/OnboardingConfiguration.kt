package io.snabble.sdk.screens.onboarding.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Global configuration for the onboarding layout
 * @param imageSource The Logo on top of each page
 */
@Parcelize
data class OnboardingConfiguration (
    val imageSource: String?,
) : Parcelable