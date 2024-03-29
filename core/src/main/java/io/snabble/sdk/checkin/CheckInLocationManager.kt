package io.snabble.sdk.checkin

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.Logger

private const val LOCATION_UPDATE_INTERVAL_MS = 5000L
private const val LOCATION_UPDATE_MIN_DISTANCE = 0f

/**
 * Location manager used by the check in manager. Periodically polls location after calling
 * startTrackingLocation and stores it in location live data
 */
class CheckInLocationManager(val application: Application) {
    private val locationManager: LocationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Contains the current location of the user.
     * Updating every 5 seconds after calling startTrackingLocation()
     */
    val location: MutableLiveData<Location?> = MutableLiveData()

    /**
     * Can be used to mock a location, in which case location updates
     * are ignored and only this location is used
     */
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

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(l: Location) {
            if (mockLocation == null) {
                val currentLocation = location.value
                if(isBetterLocation(l, currentLocation)) {
                    location.postValue(l)
                }
            }
        }

        override fun onFlushComplete(requestCode: Int) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }
    }

    /**
     * Checks if ACCESS_FINE_LOCATION is granted
     */
    fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if location permission is granted and location is enabled by the user
     */
    fun isLocationAvailable(): Boolean {
        return if (!checkLocationPermission()) {
            false
        } else isEnabled(Snabble.application)
    }

    /**
     * Checks if any GPS or NETWORK location providers are enabled
     */
    fun isEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return allowedProviders.any { lm.isProviderEnabled(it) }
    }

    /**
     * Starts polling location updates every 5 seconds and storing the result in getLocation() live data.
     *
     * Requires location permissions.
     *
     * Does nothing if location permission is not granted.
     */
    // suppressing permission because the hosting app is technically not required to provide this permission
    // if it is not using the check in manager
    @SuppressLint("MissingPermission")
    fun startTrackingLocation() {
        Dispatch.mainThread {
            if (checkLocationPermission()) {
                val providers = locationManager.getProviders(true)
                providers.retainAll(allowedProviders)
                providers.forEach {
                    locationManager.requestLocationUpdates(
                        it,
                        LOCATION_UPDATE_INTERVAL_MS,
                        LOCATION_UPDATE_MIN_DISTANCE,
                        locationListener
                    )
                }
            } else {
                Logger.e("Missing location permission, location can not be updated")
            }
        }
    }

    /**
     * Stops polling for location updates
     */
    @SuppressLint("MissingPermission")
    fun stopTrackingLocation() {
        Dispatch.mainThread {
            locationManager.removeUpdates(locationListener)
        }
    }
}
