package io.snabble.sdk.onboarding.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnboardingConfiguration (
    var imageSource: String?,
    var hasPageControl: Boolean?
) : Parcelable