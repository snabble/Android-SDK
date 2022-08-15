package io.snabble.sdk.onboarding.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OnboardingConfiguration (
    val imageSource: String?,
    val hasPageControl: Boolean?
) : Parcelable