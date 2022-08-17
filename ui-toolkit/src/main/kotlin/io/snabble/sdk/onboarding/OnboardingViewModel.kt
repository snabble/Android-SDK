package io.snabble.sdk.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OnboardingViewModel() : ViewModel(){

    val onboardingSeen: LiveData<Boolean> = MutableLiveData()

    fun onboardingFinished() {
        (onboardingSeen as MutableLiveData<Boolean>).postValue(true)
    }

    fun onboardingCancled() {
        (onboardingSeen as MutableLiveData<Boolean>).postValue(false)
    }
}