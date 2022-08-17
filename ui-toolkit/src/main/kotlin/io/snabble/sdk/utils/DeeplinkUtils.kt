@file:JvmName("UriExtension")

package io.snabble.sdk.utils

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavController

/** Parse current query params to the requested Deeplink e.g. for hiding bottom navbar*/
fun Uri.appendDeeplinkQueryParams(arguments: Bundle?): Uri {
    val currentDeeplink = (arguments?.get(NavController.KEY_DEEP_LINK_INTENT) as? Intent)?.data
    return this.buildUpon().encodedQuery(currentDeeplink?.encodedQuery).build()
}
/** Check if the uri matches app scheme and we can navigate directly internal*/
fun Uri.matchesAppScheme(arguments: Bundle?): Boolean {
    val currentDeeplink = (arguments?.get(NavController.KEY_DEEP_LINK_INTENT) as? Intent)?.data
    return this.scheme == currentDeeplink?.scheme
}
