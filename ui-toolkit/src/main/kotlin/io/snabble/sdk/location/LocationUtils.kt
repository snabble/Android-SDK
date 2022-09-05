package io.snabble.sdk.location

import android.location.Location
import com.google.android.gms.maps.model.LatLng

fun Location.toLatLng() = LatLng(latitude, longitude)

fun Float.formatDistance() = toDouble().let {
    when {
        it > 100000 -> String.format("%,.0f km", it / 1000f)
        it > 1000 -> String.format("%,.1f km", it / 1000f)
        else -> String.format("%,.0f m", it)
    }
}

fun LatLng.distanceTo(other: LatLng): Float = FloatArray(1).also {
    Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, it)
}[0]
