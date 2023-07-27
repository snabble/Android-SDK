@file:JvmName("ViolationNotificationUtils")
package io.snabble.sdk.ui.checkout

import android.content.Context
import androidx.appcompat.app.AlertDialog
import io.snabble.sdk.shoppingcart.ShoppingCart
import io.snabble.sdk.ViolationNotification
import io.snabble.sdk.ui.R
import io.snabble.sdk.ui.utils.I18nUtils

/**
 * Build a localized error message from a list of `ViolationNotification`.
 */
fun List<ViolationNotification>.getMessage(context: Context) = joinToString("\n") {
    val res = context.resources
    when (it.type) {
        "coupon_invalid" -> res.getString(I18nUtils.getIdentifier(res, R.string.Snabble_Violations_couponInvalid), it.name)
        "coupon_currently_not_valid" -> res.getString(I18nUtils.getIdentifier(res, R.string.Snabble_Violations_couponCurrentlyNotValid), it.name)
        "coupon_already_voided" -> res.getString(I18nUtils.getIdentifier(res, R.string.Snabble_Violations_couponAlreadyVoided), it.name)
        else -> it.fallbackMessage.orEmpty()
    }
}

/**
 * Show a dialog with all current violations. The implementation will make sure that the dialog won't be shown twice.
 */
fun List<ViolationNotification>.showNotificationOnce(context: Context, cart: ShoppingCart) {
    val message: String = getMessage(context)
    if (cart.violationNotifications.isNotEmpty()) {
        AlertDialog.Builder(context)
            .setTitle(R.string.Snabble_Violations_title)
            .setMessage(message)
            .setPositiveButton(R.string.Snabble_ok, null)
            .show()
    }
    cart.removeViolationNotification(this)
}
