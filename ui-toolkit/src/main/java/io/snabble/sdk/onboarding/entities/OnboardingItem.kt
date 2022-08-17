package io.snabble.sdk.onboarding.entities

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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