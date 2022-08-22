package io.snabble.sdk.shopfinder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.snabble.sdk.Project
import io.snabble.sdk.Shop

class ShopfinderViewModel : ViewModel() {

    val isCheckedIn: LiveData<Boolean> = MutableLiveData()
    val currentShop: LiveData<Shop?> = MutableLiveData()
    val currentProject: LiveData<Project?> = MutableLiveData()


    fun updateCurrentProject(project: Project?){
        (currentProject as MutableLiveData<Project?>).postValue(project)

    }
    fun updateCurrentShop(shop: Shop?){
        (currentShop as MutableLiveData<Shop?>).postValue(shop)

    }

    /** Passes if maps is enabled or not to the shopDetailsFragment */
    fun checkIn() {
        (isCheckedIn as MutableLiveData<Boolean>).postValue(true)
    }

    fun checkOut() {
        (isCheckedIn as MutableLiveData<Boolean>).postValue(false)
    }

}