package io.snabble.sdk.checkin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.*
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.Logger

class CheckInLocationManager(val application: Application) {
    private val locationManager: LocationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val location: MutableLiveData<Location?> = MutableLiveData(null)
    var mockLocation: Location? = null
        set(value) {
            value?.time = System.currentTimeMillis()
            field = value
            location.value = value
        }

    private val allowedProviders = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    )

    private val locationListener = LocationListener { l ->
        if (mockLocation == null) {
            val currentLocation = location.value
            if(isBetterLocation(l, currentLocation)) {
                location.postValue(l)
            }
        }
    }

    fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(application,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationAvailable(): Boolean {
        return if (!checkLocationPermission()) {
            false
        } else isEnabled(Snabble.getInstance().application)
    }

    fun isEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return allowedProviders.any { lm.isProviderEnabled(it) }
    }

    // suppressing permission because the hosting app is technically not required to provide this permission
    // if it is not using the check in manager
    @SuppressLint("MissingPermission")
    fun startTrackingLocation() {
        if (checkLocationPermission()) {
            val providers = locationManager.getProviders(true)
            providers.retainAll(allowedProviders)
            providers.forEach {
                locationManager.requestLocationUpdates(it, 5000, 0.0f, locationListener)
            }
        } else {
            Logger.e("Missing location permission, location can not be updated")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopTrackingLocation() {
        locationManager.removeUpdates(locationListener)
    }
}