package io.snabble.sdk

import android.location.Location
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckinManagerTest : SnabbleSdkTest() {
    @Test
    fun testCheckIn() {
        val mockLocation = Location("")
        mockLocation.setLatitude(50.7352963)
        mockLocation.setLongitude(7.100141)

        val checkInManager = Snabble.getInstance().checkInManager
        val locationManager = Snabble.getInstance().checkInLocationManager

        Assert.assertEquals("1774", checkInManager.shop.getOrAwaitValue(count = 2)?.id)
        locationManager.mockLocation = mockLocation
    }
}