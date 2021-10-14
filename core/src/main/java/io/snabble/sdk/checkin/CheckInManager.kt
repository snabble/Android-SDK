package ch.gooods.manager

import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import ch.gooods.location.CheckInLocationManager
import ch.gooods.location.distanceTo
import ch.gooods.location.toLatLng
import com.google.android.gms.maps.model.LatLng
import io.snabble.sdk.Project
import io.snabble.sdk.Shop
import io.snabble.sdk.Snabble
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

class CheckInManager(val snabble: Snabble,
                     val locationManager: CheckInLocationManager) {

    private val TAG_SHOP_ID = "shopId"
    private val TAG_CHECKIN_TIME = "checkinTime"
    private val CHECKIN_RADIUS = 500
    private val CHECKIN_STAY_CHECKED_IN_RADIUS = 1000
    private val STAY_CHECKED_IN_SECONDS = 15000L

    private val application = snabble.application

    // TODO shop/debugShop
    val shop = MutableLiveData<Shop?>(null)
    val shops = MutableLiveData<List<Shop>?>(null)

    private var lastLocation: Location? = null
    private val sharedPreferences = application.getSharedPreferences("snabble_checkin_manager", Context.MODE_PRIVATE)

    private val handler = Handler(Looper.getMainLooper())
    private var projectByShopId = HashMap<String, Project>()

    private var shopList: List<Shop> = emptyList()
    private var lastCheckedInProject: Project? = null

    init {
        locationManager.location.observeForever { location ->
            lastLocation = location
            update()
        }

        snabble.addOnMetadataUpdateListener {
            update()
        }

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
            shop.postValue(savedShop)

            if (savedShop != null) {
                shops.postValue(listOf(savedShop))
            } else {
                shops.postValue(null)
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
            val nearestShop = shops.value?.nearest(loc)
            nearestShop?.let { it ->
                if (it.distance < CHECKIN_RADIUS) {
                    checkIn(it.shop)
                } else if (it.distance < CHECKIN_STAY_CHECKED_IN_RADIUS && shop.value != null) {
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

    fun checkIn(checkInShop: Shop?) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            checkIn(null)
        }, TimeUnit.SECONDS.toMillis(STAY_CHECKED_IN_SECONDS))

        shop.postValue(checkInShop)

        if (checkInShop != null) {
            shops.postValue(listOf(checkInShop))
        } else {
            shops.postValue(null)
        }

        if (checkInShop != null) {
            val newCheckedInProject = projectByShopId.get(checkInShop.id)

            if (lastCheckedInProject != newCheckedInProject){
                lastCheckedInProject = newCheckedInProject
                newCheckedInProject?.checkedInShop = checkInShop
            }
        } else {
            lastCheckedInProject?.checkedInShop = null
            lastCheckedInProject = null
        }

        sharedPreferences.edit()
            .putString(TAG_SHOP_ID, checkInShop?.id)
            .putLong(TAG_CHECKIN_TIME, if (checkInShop != null) System.currentTimeMillis() else 0)
            .apply()

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
}

data class NearestShop(
    val distance: Float,
    val shop: Shop
)

fun List<Shop>.nearest(location: LatLng): NearestShop? {
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