package io.snabble.sdk.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.maps.model.LatLng
import java.util.*
import kotlin.math.roundToInt

object LocationUtils {

    @JvmStatic
    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    fun Location?.isSameLocation(other: Location?) = this?.latitude == other?.latitude && this?.longitude == other?.longitude && this?.altitude == other?.altitude
}
fun Location.toLatLng() = LatLng(latitude, longitude)

fun Float.formatDistance() = toDouble().let {
    when {
        it > 100000 -> it.formatThousands()
        it > 1000 -> String.format("%.1f km", it / 1000f)
        else -> String.format("%.0f m", it)
    }
}

fun LatLng.distanceTo(other: LatLng): Float = FloatArray(1).also {
    Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, it)
}[0]

fun LatLng.formatted() = String.format(Locale.US, "%1.6f,%1.6f", latitude, longitude)

fun Double.formatThousands(): String {
    val distance = (this / 1000).roundToInt().toString()
    val lastIndex = distance.length
    return String.format(
        "%s.%s km",
        distance.substring(0, lastIndex - 3),
        distance.substring(lastIndex - 3, lastIndex)
    )
}