package io.snabble.sdk.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.Task
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.Logger
import io.snabble.sdk.utils.logDebug

class LocationManager internal constructor(private val context: Context) {
    companion object{

        private const val PREFS_KEY_PERMANENTLY_DENIED = "permanently_denied"
        private const val KEY_MOCK_LOCATION = "mock_location"

        @SuppressLint("StaticFieldLeak")
        private var instance: io.snabble.sdk.location.LocationManager? = null

        fun getInstance(context: Context): io.snabble.sdk.location.LocationManager {
            if (instance == null){
                instance = LocationManager(context)
            }
            return instance!!
        }
    }

    private var locSharedPreferences: SharedPreferences =
        context.getSharedPreferences("location_manager", Context.MODE_PRIVATE)
    private var prefSharedPreferences: SharedPreferences =
        context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    private var userHasPermanentlyDeniedPermission: Boolean

    @SuppressLint("StaticFieldLeak") // we use application context, so this is fine
    private val fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var isLocationAvailable: Boolean
    val location: MutableLiveData<Location?> = MutableLiveData(null)

    var mockLocation: String?
        get() = locSharedPreferences.getString(KEY_MOCK_LOCATION, null)
        set(latLng) {
            locSharedPreferences
                .edit()
                .putString(KEY_MOCK_LOCATION, latLng)
                .apply()
        }

    private val locationCallback: LocationCallback = object : LocationCallback() {

        override fun onLocationResult(locationResult: LocationResult) {
            val checkInLocationManager = Snabble.checkInLocationManager
            var mock = mockLocation
            if (mock != null) {
                try {
                    mock = mockLocation
                    if (mock != null) {
                        val latLng = mock.split(";")
                        val loc = Location("static").apply {
                            latitude = Location.convert(latLng[0])
                            longitude = Location.convert(latLng[1])
                        }
                        location.postValue(loc)
                        checkInLocationManager.mockLocation = loc

                        Logger.d("Using mock location $location")
                    }

                } catch (e: Exception) {
                    Logger.d("Error parsing mock location $e")
                    checkInLocationManager.mockLocation = null
                    location.postValue(locationResult.lastLocation)
                }
            } else {
                checkInLocationManager.mockLocation = null
                location.postValue(locationResult.lastLocation)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            logDebug(locationAvailability.toString())
            isLocationAvailable = locationAvailability.isLocationAvailable
        }
    }

    fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationAvailable(): Boolean {
        return if (!checkLocationPermission()) {
            false
        } else isEnabled(context) || isLocationAvailable
    }

    fun isEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    fun startTrackingLocation() {
        if (checkLocationPermission()) {
            val locationRequest = LocationRequest.create()
            locationRequest.interval = 5000
            locationRequest.priority = PRIORITY_HIGH_ACCURACY

            logDebug("startLocationUpdates")
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    fun stopTrackingLocation() {
        logDebug("stopLocationUpdates")
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    fun isPermissionPermanentlyDenied(): Boolean {
        return userHasPermanentlyDeniedPermission
    }

    fun setUserHasPermanentlyDeniedPermission(userHasPermanentlyDenied: Boolean) {
        userHasPermanentlyDeniedPermission = userHasPermanentlyDenied
        prefSharedPreferences.edit().putBoolean(PREFS_KEY_PERMANENTLY_DENIED, true).apply()
    }

    fun getLastLocation(): Location? {
        return location.value
    }

    init {
        userHasPermanentlyDeniedPermission = locSharedPreferences.getBoolean(
            PREFS_KEY_PERMANENTLY_DENIED, false)
        isLocationAvailable = false
        if (checkLocationPermission()) {
            fusedLocationProviderClient.locationAvailability.addOnCompleteListener { task: Task<LocationAvailability> ->
                if(task.isSuccessful) {
                    task.result?.let {
                        isLocationAvailable = it.isLocationAvailable
                    }

                    fusedLocationProviderClient.lastLocation.addOnSuccessListener { newLocation: Location? ->
                        location.postValue(newLocation)
                    }
                } else {
                    isLocationAvailable = false
                }
            }
        }
    }
}