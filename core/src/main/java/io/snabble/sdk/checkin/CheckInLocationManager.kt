package ch.gooods.location

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
    private val locationManager: LocationManager

    val location: MutableLiveData<Location?> = MutableLiveData(null)
    var mockLocation: Location? = null
        set(value) {
            mockLocation = value
            location.postValue(value)
        }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(l: Location) {
            if (mockLocation != null) {
                location.postValue(l)
            }
        }
    }

    init {
        locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(application,
            Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED
    }

    fun isLocationAvailable(): Boolean {
        return if (!checkLocationPermission()) {
            false
        } else isEnabled(Snabble.getInstance().application)
    }

    fun isEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    fun startTrackingLocation() {
        if (checkLocationPermission()) {
            val criteria = Criteria();
            criteria.accuracy = Criteria.ACCURACY_FINE
            criteria.isCostAllowed = false
            criteria.isAltitudeRequired = false
            criteria.isBearingRequired = false

            val provider = locationManager.getBestProvider(criteria, true);
            if (provider != null) {
                locationManager.requestLocationUpdates(provider, 5000, 10.0f, locationListener)
            } else {
                Logger.e("No suitable location provider available, location can not be updated")
            }
        } else {
            Logger.e("Missing location permission, location can not be updated")
        }
    }

    fun stopTrackingLocation() {
        locationManager.removeUpdates(locationListener)
    }
}