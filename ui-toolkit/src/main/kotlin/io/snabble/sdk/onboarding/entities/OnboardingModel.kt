package io.snabble.sdk.onboarding.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnboardingModel(
    val configuration: OnboardingConfiguration,
    val items: List<OnboardingItem>
) : Parcelable