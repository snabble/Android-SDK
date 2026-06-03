package io.snabble.sdk.screens.onboarding.data

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Holds the configuration to set up the onboarding layout
 */
@Parcelize
data class OnboardingModel(
    @SerializedName ("configuration")val configuration: OnboardingConfiguration?,
    @SerializedName ("items")val items: List<OnboardingItem>
) : Parcelable
