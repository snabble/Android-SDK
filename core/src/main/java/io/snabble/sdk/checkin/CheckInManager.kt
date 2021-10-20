package io.snabble.sdk.checkin

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Observer
import com.google.android.gms.maps.model.LatLng
import io.snabble.sdk.Project
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import io.snabble.sdk.utils.Dispatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

interface OnCheckInStateChangedListener {
    fun onCheckInStateChanged()
}

private const val TAG_SHOP_ID = "shop_id"
private const val TAG_CHECKIN_TIME = "checkin_time"

class CheckInManager(val snabble: Snabble,
                     val locationManager: CheckInLocationManager) {
    private val CHECKIN_RADIUS = 500
    private val CHECKIN_STAY_CHECKED_IN_RADIUS = 1000
    private val STAY_CHECKED_IN_SECONDS = 15000L

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
        get() {
            return projectByShopId.get(shop?.id)
        }
        private set;

    var candidates: List<Shop>? = null

    var checkedInAt: Long = -1

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

        val savedTime = sharedPreferences.getLong(TAG_CHECKIN_TIME, 0)

        val now = System.currentTimeMillis()
        val savedTimeDiff = now - savedTime
        val locationAge = now - (lastLocation?.time ?: 0)
        val locationAccuracy = lastLocation?.accuracy ?: 10000f

        val canUseSavedShop = savedTime != 0L
                    && savedTimeDiff < TimeUnit.SECONDS.toMillis(STAY_CHECKED_IN_SECONDS)
                    && locationAge < 60000
                    && locationAccuracy < CHECKIN_RADIUS

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
                if (it.distance < CHECKIN_RADIUS) {
                    checkIn(it.shop)
                } else if (it.distance < CHECKIN_STAY_CHECKED_IN_RADIUS && currentShop?.id == it.shop.id) {
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
        }, TimeUnit.SECONDS.toMillis(STAY_CHECKED_IN_SECONDS))

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

        checkedInAt = if (checkInShop != null) System.currentTimeMillis() else 0

        sharedPreferences.edit()
            .putString(TAG_SHOP_ID, checkInShop?.id)
            .putLong(TAG_CHECKIN_TIME, checkedInAt)
            .apply()

        notifyOnCheckInStateChanged()
        println("checkin to " + checkInShop?.id)
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

    private fun notifyOnCheckInStateChanged() {
        Dispatch.mainThread {
            onCheckInStateChangedListeners.forEach {
                it.onCheckInStateChanged()
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