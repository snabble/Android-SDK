package io.snabble.sdk.screens.onboarding.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Holds the configuration to set up the onboarding layout
 */
@Parcelize
data class OnboardingModel(
    val configuration: OnboardingConfiguration?,
    val items: List<OnboardingItem>
) : Parcelable