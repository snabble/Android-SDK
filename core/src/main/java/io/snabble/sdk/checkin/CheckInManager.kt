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

/**
 * Listener for check in states. Notifies about check in / check out and if multiple shops are
 * available in the same area
 */
interface OnCheckInStateChangedListener {
    /**
     * Gets called when the user is inside or near a shop.
     */
    fun onCheckIn(shop: Shop)

    /**
     * Gets called after the user leaves the area of a shop for lastSeenThreshold time.
     */
    fun onCheckOut()

    /**
     * Gets called if the check in manager detected multiple shops in the same area.
     * You should show a message to the user for selecting one of the available shops.
     *
     * You can set the Shop via CheckInManager#setShop and it will be the checked in shop
     * until the user leaves the area.
     */
    fun onMultipleCandidatesAvailable(candidates: List<Shop>)
}

private const val TAG_SHOP_ID = "shop_id"
private const val TAG_CHECKIN_TIME = "checkin_time"

/**
 * Provides functionality for checking in at shops using the users Location.
 */
class CheckInManager(val snabble: Snabble,
                     val locationManager: CheckInLocationManager,
                     var checkInRadius: Float = 500.0f,
                     var checkOutRadius: Float = 1000.0f,
                     var lastSeenThreshold: Long = TimeUnit.MINUTES.toMillis(15)) {
    private val application = snabble.application
    private val sharedPreferences = application.getSharedPreferences("snabble_checkin_manager", Context.MODE_PRIVATE)

    private var lastLocation: Location? = null

    private val handler = Handler(Looper.getMainLooper())
    private var projectByShopId = mapOf<String, Project>()
    private var shopList: List<Shop> = emptyList()
    private var lastCheckedInProject: Project? = null

    private val onCheckInStateChangedListeners = CopyOnWriteArrayList<OnCheckInStateChangedListener>()

    private var currentShop: Shop? = null

    /**
     * Set's the current checked in Shop.
     *
     * If you set a shop while still updating it may be updated.
     */
    var shop: Shop?
        get() = currentShop
        set(value) {
            checkIn(value)
        }

    /**
     * The project associated with the currently checked in shop. Is null if not checked in a shop.
     */
    val project: Project?
        get() = projectByShopId[shop?.id]

    /**
     * Potential list of shops, if in range of multiple shops.
     * To select a shop of multiple candidates you need to call setShop.
     *
     * While still inside the checkOutRadius the selected shop will be preferred.
     */
    var candidates: List<Shop>? = null

    /**
     * The time at which the last successful check in was made. Updates itself to the current time
     * while still inside the check in/out radius.
     */
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

    private val metadataListener = Snabble.OnMetadataUpdateListener {
        update()
    }

    fun update() {
        updateShopProjectsMap()

        shopList = snabble.projects.flatMap { it.shops }

        val savedTime = checkedInAt

        val now = System.currentTimeMillis()
        val savedTimeDiff = now - savedTime

        val canUseSavedShop = savedTime != 0L
                    && savedTimeDiff < lastSeenThreshold

        if (canUseSavedShop) {
            val savedShop = shopList.firstOrNull {
                it.id == sharedPreferences.getString(TAG_SHOP_ID, null)
            }

            checkIn(savedShop)

            Logger.d("Using saved shop " + savedShop?.id)
        }

        lastLocation?.let { loc ->
            val locationAge = now - (loc.time ?: 0)
            val locationAccuracy = loc.accuracy ?: 10000f

            if (locationAge < 60000 && locationAccuracy < checkInRadius) {
                val newCandidates = shopList.filter { it.location.distanceTo(loc) < checkInRadius }

                if (candidates?.map { it.id } != newCandidates.map { it.id }) {
                    candidates = newCandidates

                    if (newCandidates.size > 1) {
                        notifyOnMultipleCandidatesAvailable(newCandidates)
                    }
                }

                val currentShop = shop
                val distance = if (currentShop == null) {
                    -1.0f
                } else {
                    loc.distanceTo(currentShop.location)
                }

                if (distance < 0 || distance > checkOutRadius) {
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
                }
            }
        } ?: run {
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
     * Requires ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION.
     */
    @RequiresPermission(anyOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ])

    fun startUpdating() {
        Dispatch.mainThread {
            locationManager.startTrackingLocation()
            locationManager.location.observeForever(locationObserver)
            Snabble.addOnMetadataUpdateListener(metadataListener)
        }
    }

    fun stopUpdating() {
        Dispatch.mainThread {
            locationManager.stopTrackingLocation()
            locationManager.location.removeObserver(locationObserver)
            Snabble.removeOnMetadataUpdateListener(metadataListener)
        }
    }

    private fun checkIn(checkInShop: Shop?) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            checkIn(null)
        }, lastSeenThreshold)

        if (currentShop != checkInShop) {
            currentShop = checkInShop

            if (checkInShop != null) {
                val newCheckedInProject = projectByShopId[checkInShop.id]

                if (lastCheckedInProject != newCheckedInProject) {
                    lastCheckedInProject = newCheckedInProject
                    Snabble.checkedInShop = null
                }

                Snabble.checkedInShop = checkInShop
            } else {
                Snabble.checkedInShop = null
                lastCheckedInProject = null
            }

            sharedPreferences.edit()
                .putString(TAG_SHOP_ID, checkInShop?.id)
                .putLong(
                    TAG_CHECKIN_TIME,
                    if (checkInShop != null) System.currentTimeMillis() else 0
                )
                .apply()

            if (checkInShop != null) {
                notifyOnCheckIn(checkInShop)
            } else {
                notifyOnCheckOut()
            }

            Logger.d("check in to " + checkInShop?.id)
        }
    }


    private fun updateShopProjectsMap() {
        val projects = Snabble.projects
        projectByShopId = projects.flatMap { project -> project.shops.map { it.id to project } }.toMap()
    }

    fun addOnCheckInStateChangedListener(onCheckInStateChangedListener: OnCheckInStateChangedListener) {
        onCheckInStateChangedListeners.add(onCheckInStateChangedListener)

        val shop = shop
        val project = project

        if (shop != null && project != null) {
            onCheckInStateChangedListener.onCheckIn(shop)
        }
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

    private fun notifyOnMultipleCandidatesAvailable(candidates: List<Shop>) {
        Dispatch.mainThread {
            onCheckInStateChangedListeners.forEach {
                it.onMultipleCandidatesAvailable(candidates)
            }
        }
    }
}

private data class NearestShop(
    val distance: Float,
    val shop: Shop
)

private fun List<Shop>.nearest(location: Location): NearestShop? {
    val nearest = minByOrNull { it.location.distanceTo(location) }
    val nearestDistance = nearest?.location?.distanceTo(location)

    if (nearest != null && nearestDistance != null) {
        return NearestShop(nearestDistance, nearest)
    }

    return null
}