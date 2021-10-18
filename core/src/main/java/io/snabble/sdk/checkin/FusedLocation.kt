package io.snabble.sdk.checkin

import android.location.Location

private const val TWO_MINUTES = 1000 * 60 * 2

/** Determines whether one Location reading is better than the current Location fix
 * @param location  The new Location that you want to evaluate
 * @param currentBestLocation  The current Location fix, to which you want to compare the new one
 */
fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
    if (currentBestLocation == null) {
        // A new location is always better than no location
        return true
    }

    // Check whether the new location fix is newer or older
    val timeDelta = location.time - currentBestLocation.time
    val isSignificantlyNewer = timeDelta > TWO_MINUTES
    val isSignificantlyOlder = timeDelta < -TWO_MINUTES
    val isNewer = timeDelta > 0

    // If it's been more than two minutes since the current location, use the new location
    // because the user has likely moved
    if (isSignificantlyNewer) {
        return true
        // If the new location is more than two minutes older, it must be worse
    } else if (isSignificantlyOlder) {
        return false
    }

    // Check whether the new location fix is more or less accurate
    val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
    val isLessAccurate = accuracyDelta > 0
    val isMoreAccurate = accuracyDelta < 0
    val isSignificantlyLessAccurate = accuracyDelta > 200

    // Check if the old and new location are from the same provider
    val isFromSameProvider = isSameProvider(
        location.provider,
        currentBestLocation.provider
    )

    // Determine location quality using a combination of timeliness and accuracy
    if (isMoreAccurate) {
        return true
    } else if (isNewer && !isLessAccurate) {
        return true
    } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
        return true
    }
    return false
}

/** Checks whether two providers are the same  */
private fun isSameProvider(provider1: String?, provider2: String?): Boolean {
    return if (provider1 == null) {
        provider2 == null
    } else provider1 == provider2
}