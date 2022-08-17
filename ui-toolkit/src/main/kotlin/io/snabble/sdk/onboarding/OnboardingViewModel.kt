package io.snabble.sdk.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** Viewmodel to hold the state of the onboarding progress */
class OnboardingViewModel() : ViewModel(){

    val onboardingSeen: LiveData<Boolean> = MutableLiveData()

    /** Updates the onboarding progress. Can be observed to handle the finished event */
    fun onboardingFinished() {
        (onboardingSeen as MutableLiveData<Boolean>).postValue(true)
    }

    fun onboardingCancled() {
        (onboardingSeen as MutableLiveData<Boolean>).postValue(false)
    }
}