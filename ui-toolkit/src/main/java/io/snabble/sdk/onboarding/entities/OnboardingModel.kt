package io.snabble.sdk.onboarding.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnboardingModel(
    var configuration: OnboardingConfiguration,
    var items: List<OnboardingItem>
) : Parcelable