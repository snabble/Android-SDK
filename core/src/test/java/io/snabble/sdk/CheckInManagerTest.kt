package io.snabble.sdk

import android.location.Location
import io.snabble.sdk.checkin.CheckInManager
import io.snabble.sdk.checkin.OnCheckInStateChangedListener
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class CheckInManagerTest : SnabbleSdkTest() {
    val locationSnabble = Location("").apply {
        latitude = 50.7352963
        longitude = 7.100141
        accuracy = 0.0f
        time = System.currentTimeMillis()
    }

    val locationSnabbleButWith640MetersDistance = Location("").apply {
        latitude = 50.7340824
        longitude = 7.0912753
        accuracy = 0.0f
        time = System.currentTimeMillis()
    }

    val locationNoWhere = Location("").apply {
        latitude = 51.7352963
        longitude = 5.100141
        accuracy = 0.0f
        time = System.currentTimeMillis()
    }

    override fun onApplyConfig(config: Snabble.Config) {
        super.onApplyConfig(config)

        config.checkInRadius = 500.0f;
        config.checkOutRadius = 1000.0f;
        config.lastSeenThreshold = TimeUnit.MINUTES.toMillis(15);
    }

    @Test
    fun testCheckIn() {
        val checkInManager = Snabble.getInstance().checkInManager
        checkInManager.startUpdating()

        val listener = CheckInListener(checkInManager, onMultipleCandidatesAvailable = {
            countDown()
        })

        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = locationSnabble

        listener.await()
        Assert.assertEquals("1774", checkInManager.shop?.id)
    }

    @Test
    fun testCheckOutByDistanceAndTime() {
        val checkInManager = Snabble.getInstance().checkInManager
        checkInManager.startUpdating()

        val listener = CheckInListener(checkInManager, onMultipleCandidatesAvailable = {
            countDown()
        })

        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = locationSnabble

        listener.await()
        Assert.assertEquals("1774", checkInManager.shop?.id)

        // simulate that we checked in 15 minutes before
        checkInManager.checkedInAt = checkInManager.checkedInAt - TimeUnit.MINUTES.toMillis(15)

        val listener2 = CheckInListener(checkInManager, onCheckOut = {
            countDown()
        })

        locationManager.mockLocation = locationNoWhere
        listener2.await()
        Assert.assertEquals(null, checkInManager.shop?.id)
    }

    @Test
    fun testStillCheckedInWhileInTimeWindow() {
        val checkInManager = Snabble.getInstance().checkInManager
        checkInManager.startUpdating()

        val listener = CheckInListener(checkInManager, onMultipleCandidatesAvailable = {
            countDown()
        })

        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = locationSnabble

        listener.await()
        Assert.assertEquals("1774", checkInManager.shop?.id)
        locationManager.mockLocation = locationNoWhere

        val listener2 = CheckInListener(checkInManager,
            onCheckIn = {
                if (it.id != "1774") {
                    Assert.fail("Shop is different, but should be the same!")
                }},
            onCheckOut = {
                Assert.fail("Shop should still be checked in, but is not")
            })

        locationManager.mockLocation = locationNoWhere
        listener2.await()
    }

    @Test
    fun testStillCheckedInWhileInCheckOutRadiusButNotInTimeWindow() {
        val checkInManager = Snabble.getInstance().checkInManager
        checkInManager.startUpdating()

        val listener = CheckInListener(checkInManager, onMultipleCandidatesAvailable = {
            countDown()
        })

        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = locationSnabble

        listener.await()
        Assert.assertEquals("1774", checkInManager.shop?.id)

        // simulate that we checked in 15 minutes before
        checkInManager.checkedInAt = checkInManager.checkedInAt - TimeUnit.MINUTES.toMillis(15)

        val listener2 = CheckInListener(checkInManager,
            onCheckIn = {
                if (it.id != "1774") {
                    Assert.fail("Shop is different, but should be the same!")
                }},
            onCheckOut = {
                Assert.fail("Shop should still be checked in, but is not")
            })

        locationManager.mockLocation = locationSnabbleButWith640MetersDistance
        listener2.await()
    }

    @Test
    fun testMultipleShopsAvailable() {
        val checkInManager = Snabble.getInstance().checkInManager
        checkInManager.startUpdating()

        val listener = CheckInListener(checkInManager, onMultipleCandidatesAvailable = {
            countDown()
        })

        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = locationSnabble

        listener.await()
        Assert.assertEquals(2, checkInManager.candidates?.size)
    }

    @Test
    fun testMultipleShopsCheckIn() {
        val checkInManager = Snabble.getInstance().checkInManager
        checkInManager.startUpdating()

        val listener = CheckInListener(checkInManager, onCheckIn = {
            countDown()
        })

        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = locationSnabble

        listener.await()
        Assert.assertEquals("1774", checkInManager.shop?.id)

        val listener2 = CheckInListener(checkInManager, onCheckIn = {
            countDown()
        })

        checkInManager.shop = project.shops.find { it.id == "1337" }
        locationManager.mockLocation = locationSnabble
        listener2.await()
        Assert.assertEquals("1337", checkInManager.shop?.id)
    }
}

private class CheckInListener(
    checkInManager: CheckInManager,
    onCheckIn: (CheckInListener.(Shop)->Any)? = null,
    onCheckOut: (CheckInListener.()->Any)? = null,
    onMultipleCandidatesAvailable: (CheckInListener.(List<Shop>)->Any)? = null) {
    private val latch = CountDownLatch(1)

    init {
        checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckIn(shop: Shop) {
                onCheckIn?.invoke(this@CheckInListener, shop)
            }

            override fun onCheckOut() {
                onCheckOut?.invoke(this@CheckInListener)
            }

            override fun onMultipleCandidatesAvailable(candidates: List<Shop>) {
                onMultipleCandidatesAvailable?.invoke(this@CheckInListener, candidates)
            }
        })
    }

    fun await() = latch.await(2, TimeUnit.SECONDS)

    fun countDown() = latch.countDown()
}