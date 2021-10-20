package io.snabble.sdk.checkin

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.lifecycle.Observer
import com.google.android.gms.maps.model.LatLng
import io.snabble.sdk.Project
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.Dispatch
import io.snabble.sdk.utils.Logger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

interface OnCheckInStateChangedListener {
    fun onCheckIn(shop: Shop)
    fun onCheckOut()
}

private const val TAG_SHOP_ID = "shop_id"
private const val TAG_CHECKIN_TIME = "checkin_time"

class CheckInManager(val snabble: Snabble,
                     val locationManager: CheckInLocationManager,
                     var checkInRadius: Float = 500.0f,
                     var checkOutRadius: Float = 1000.0f,
                     var lastSeenThreshold: Long = TimeUnit.MINUTES.toMillis(15)) {
    private val application = snabble.application
    private val sharedPreferences = application.getSharedPreferences("snabble_checkin_manager", Context.MODE_PRIVATE)

    private var lastLocation: Location? = null

    private val handler = Handler(Looper.getMainLooper())
    private var projectByShopId = HashMap<String, Project>()
    private var shopList: List<Shop> = emptyList()
    private var lastCheckedInProject: Project? = null

    private val onCheckInStateChangedListeners = CopyOnWriteArrayList<OnCheckInStateChangedListener>()

    private var currentShop: Shop? = null

    var shop: Shop?
        get() = currentShop
        set(value) {
            currentShop = value
            checkIn(value)
        }

    var project: Project? = null
        get() = projectByShopId.get(shop?.id)
        private set

    var candidates: List<Shop>? = null

    var checkedInAt: Long
        get() = sharedPreferences.getLong(TAG_CHECKIN_TIME, 0)
        internal set(value) = sharedPreferences.edit()
            .putLong(TAG_CHECKIN_TIME, value)
            .apply()

    private val locationObserver =
        Observer<Location?> { location ->
            lastLocation = location
            update()
        }

    private val metadataListener = object : Snabble.OnMetadataUpdateListener {
        override fun onMetaDataUpdated() {
            update()
        }
    }

    init {
        update()
    }

    private fun update() {
        updateShopProjectsMap()

        shopList = snabble.projects.flatMap { it.shops }

        val savedTime = checkedInAt

        val now = System.currentTimeMillis()
        val savedTimeDiff = now - savedTime
        val locationAge = now - (lastLocation?.time ?: 0)
        val locationAccuracy = lastLocation?.accuracy ?: 10000f

        val canUseSavedShop = savedTime != 0L
                    && savedTimeDiff < lastSeenThreshold
                    && locationAge < 60000
                    && locationAccuracy < checkInRadius

        if (canUseSavedShop) {
            val savedShop = shopList.firstOrNull {
                it.id == sharedPreferences.getString(TAG_SHOP_ID, null)
            }

            currentShop = savedShop

            if (savedShop != null) {
                candidates = listOf(savedShop)
            } else {
                candidates = null
            }

            val newCheckedInProject = projectByShopId.get(savedShop?.id)

            if (lastCheckedInProject != newCheckedInProject){
                lastCheckedInProject = newCheckedInProject
                newCheckedInProject?.checkedInShop = savedShop
            }

            println("using saved shop" + savedShop?.id)
        }

        val loc = lastLocation?.toLatLng()
        if (loc != null) {
            val currentShop = shop
            val nearestShop = shopList.nearest(loc)
            nearestShop?.let { it ->
                if (it.distance < checkInRadius) {
                    checkIn(it.shop)
                } else if (it.distance < checkOutRadius && currentShop?.id == it.shop.id) {
                    checkIn(it.shop)
                } else {
                    if (!canUseSavedShop) {
                        checkIn(null)
                    }
                }
            }
        } else {
            if (!canUseSavedShop) {
                checkIn(null)
            }
        }
    }

    /**
     * Starts tracking user's location using gps and network providers.
     *
     * Calls OnCheckInStateChangedListener's onCheckIn or onCheckOut if users enter or exit shop areas.
     *
     * Requires ACCESS_FINE_LOCATION.
     */
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startUpdating() {
        locationManager.startTrackingLocation()
        locationManager.location.observeForever(locationObserver)
        Snabble.getInstance().addOnMetadataUpdateListener(metadataListener)
    }

    fun stopUpdating() {
        locationManager.stopTrackingLocation()
        locationManager.location.removeObserver(locationObserver)
        Snabble.getInstance().removeOnMetadataUpdateListener(metadataListener)
    }

    fun checkIn(checkInShop: Shop?) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            checkIn(null)
        }, lastSeenThreshold)

        currentShop = checkInShop

        if (checkInShop != null) {
            candidates = listOf(checkInShop)
        } else {
            candidates = null
        }

        if (checkInShop != null) {
            val newCheckedInProject = projectByShopId.get(checkInShop.id)

            if (lastCheckedInProject != newCheckedInProject){
                lastCheckedInProject = newCheckedInProject
                lastCheckedInProject?.checkedInShop = null
            }

            newCheckedInProject?.checkedInShop = checkInShop
        } else {
            lastCheckedInProject?.checkedInShop = null
            lastCheckedInProject = null
        }

        sharedPreferences.edit()
            .putString(TAG_SHOP_ID, checkInShop?.id)
            .putLong(TAG_CHECKIN_TIME, if (checkInShop != null) System.currentTimeMillis() else 0)
            .apply()

        if (checkInShop != null) {
            notifyOnCheckIn(checkInShop)
        } else {
            notifyOnCheckOut()
        }

        Logger.d("check in to " + checkInShop?.id)
    }

    private fun updateShopProjectsMap() {
        val newProjectByShop = HashMap<String, Project>()

        for (project in Snabble.getInstance().projects) {
            for (s in project.shops) {
                newProjectByShop[s.id] = project
            }
        }

        projectByShopId = newProjectByShop
    }

    fun addOnCheckInStateChangedListener(onCheckInStateChangedListener: OnCheckInStateChangedListener) {
        onCheckInStateChangedListeners.add(onCheckInStateChangedListener)
    }

    fun removeOnCheckInStateChangedListener(onCheckInStateChangedListener: OnCheckInStateChangedListener) {
        onCheckInStateChangedListeners.remove(onCheckInStateChangedListener)
    }

    private fun notifyOnCheckIn(shop: Shop) {
        Dispatch.mainThread {
            onCheckInStateChangedListeners.forEach {
                it.onCheckIn(shop)
            }
        }
    }

    private fun notifyOnCheckOut() {
        Dispatch.mainThread {
            onCheckInStateChangedListeners.forEach {
                it.onCheckOut()
            }
        }
    }
}

private data class NearestShop(
    val distance: Float,
    val shop: Shop
)

private fun List<Shop>.nearest(location: LatLng): NearestShop? {
    var nearest: Shop? = null
    var nearestDistance: Float = Float.MAX_VALUE

    forEach {
        val latLng = LatLng(it.latitude, it.longitude)
        val dist = latLng.distanceTo(location)
        if (dist < nearestDistance) {
            nearest = it
            nearestDistance = dist
        }
    }

    return nearest?.let { shop ->
        NearestShop(nearestDistance, shop)
    }
}