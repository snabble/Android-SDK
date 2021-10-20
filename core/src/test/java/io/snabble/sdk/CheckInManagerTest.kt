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
    @Test
    fun testCheckIn() {
        val mockLocation = Location("")
        mockLocation.latitude = 50.7352963
        mockLocation.longitude = 7.100141

        val checkInManager = Snabble.getInstance().checkInManager
        val locationManager = Snabble.getInstance().checkInLocationManager
        locationManager.mockLocation = mockLocation

        val latch = CountDownLatch(1)
        checkInManager.addOnCheckInStateChangedListener(object : OnCheckInStateChangedListener {
            override fun onCheckInStateChanged() {
                latch.countDown()
            }
        })
        latch.await(10, TimeUnit.SECONDS)
        Assert.assertEquals("1774", checkInManager.shop?.id)
    }
}