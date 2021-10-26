package io.snabble.sdk

import android.location.Location
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

        val latch = CountDownLatch(1)
        checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckIn(shop: Shop) {
                latch.countDown()
            }

            override fun onCheckOut() {

            }

            override fun onMultipleCandidatesAvailable(candidates: List<Shop>) {

            }
        })

        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = locationSnabble

        latch.await(2, TimeUnit.SECONDS)
        Assert.assertEquals("1774", checkInManager.shop?.id)
    }

    @Test
    fun testCheckOutByDistanceAndTime() {
        val checkInManager = Snabble.getInstance().checkInManager
        checkInManager.startUpdating()

        val latch = CountDownLatch(1)
        checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckIn(shop: Shop) {
                latch.countDown()
            }

            override fun onCheckOut() {

            }

            override fun onMultipleCandidatesAvailable(candidates: List<Shop>) {

            }
        })

        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = locationSnabble

        latch.await(2, TimeUnit.SECONDS)
        Assert.assertEquals("1774", checkInManager.shop?.id)

        // simulate that we checked in 15 minutes before
        checkInManager.checkedInAt = checkInManager.checkedInAt - TimeUnit.MINUTES.toMillis(15)

        val latch2 = CountDownLatch(1)
        checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckIn(shop: Shop) {

            }

            override fun onCheckOut() {
                latch2.countDown()
            }

            override fun onMultipleCandidatesAvailable(candidates: List<Shop>) {

            }
        })

        locationManager.mockLocation = locationNoWhere
        latch2.await(2, TimeUnit.SECONDS)
        Assert.assertEquals(null, checkInManager.shop?.id)
    }

    @Test
    fun testStillCheckedInWhileInTimeWindow() {
        val checkInManager = Snabble.getInstance().checkInManager
        checkInManager.startUpdating()

        val latch = CountDownLatch(1)
        checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckIn(shop: Shop) {
                latch.countDown()
            }

            override fun onCheckOut() {

            }

            override fun onMultipleCandidatesAvailable(candidates: List<Shop>) {

            }
        })

        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = locationSnabble

        latch.await(2, TimeUnit.SECONDS)
        Assert.assertEquals("1774", checkInManager.shop?.id)
        locationManager.mockLocation = locationNoWhere

        val latch2 = CountDownLatch(1)
        checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckIn(shop: Shop) {
                if (shop.id != "1774") {
                    Assert.fail("Shop is different, but should be the same!")
                }
            }

            override fun onCheckOut() {
                Assert.fail("Shop should still be checked in, but is not")
            }

            override fun onMultipleCandidatesAvailable(candidates: List<Shop>) {

            }
        })

        locationManager.mockLocation = locationNoWhere
        latch2.await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testStillCheckedInWhileInCheckOutRadiusButNotInTimeWindow() {
        val checkInManager = Snabble.getInstance().checkInManager
        checkInManager.startUpdating()

        val latch = CountDownLatch(1)
        checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckIn(shop: Shop) {
                latch.countDown()
            }

            override fun onCheckOut() {

            }

            override fun onMultipleCandidatesAvailable(candidates: List<Shop>) {

            }
        })

        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = locationSnabble

        latch.await(2, TimeUnit.SECONDS)
        Assert.assertEquals("1774", checkInManager.shop?.id)

        // simulate that we checked in 15 minutes before
        checkInManager.checkedInAt = checkInManager.checkedInAt - TimeUnit.MINUTES.toMillis(15)

        val latch2 = CountDownLatch(1)
        checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckIn(shop: Shop) {
                if (shop.id != "1774") {
                    Assert.fail("Shop is different, but should be the same!")
                }
            }

            override fun onCheckOut() {
                Assert.fail("Shop should still be checked in, but is not")
            }

            override fun onMultipleCandidatesAvailable(candidates: List<Shop>) {

            }
        })

        locationManager.mockLocation = locationSnabbleButWith640MetersDistance
        latch2.await(2, TimeUnit.SECONDS)
    }

    @Test
    fun testMultipleShopsAvailable() {
        val checkInManager = Snabble.getInstance().checkInManager
        checkInManager.startUpdating()

        val latch = CountDownLatch(1)
        checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckIn(shop: Shop) {

            }

            override fun onCheckOut() {

            }

            override fun onMultipleCandidatesAvailable(candidates: List<Shop>) {
                latch.countDown()
            }
        })

        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = locationSnabble

        latch.await(2, TimeUnit.SECONDS)
        Assert.assertEquals(2, checkInManager.candidates?.size)
    }

    @Test
    fun testMultipleShopsCheckIn() {
        val checkInManager = Snabble.getInstance().checkInManager
        checkInManager.startUpdating()

        val latch = CountDownLatch(1)
        checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckIn(shop: Shop) {
                latch.countDown()
            }

            override fun onCheckOut() {

            }

            override fun onMultipleCandidatesAvailable(candidates: List<Shop>) {

            }
        })

        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = locationSnabble

        latch.await(2, TimeUnit.SECONDS)
        Assert.assertEquals("1774", checkInManager.shop?.id)

        val latch2 = CountDownLatch(1)
        checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckIn(shop: Shop) {
                latch2.countDown()
            }

            override fun onCheckOut() {

            }

            override fun onMultipleCandidatesAvailable(candidates: List<Shop>) {

            }
        })

        checkInManager.shop = project.shops.find { it.id == "1337" }
        locationManager.mockLocation = locationSnabble
        latch2.await(2, TimeUnit.SECONDS)
        Assert.assertEquals("1337", checkInManager.shop?.id)
    }
}