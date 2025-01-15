package io.snabble.sdk

import io.snabble.sdk.checkout.ViolationType

/**
 * A notification that a violation occurred
 */
data class ViolationNotification(
    /** The name of the affected violation item, e.g. a coupon **/
    val name: String?,
    /** The local generated uuid of the affected item **/
    val refersTo: String?,
    /** The type of the violation **/
    val type: ViolationType? = null,
    /** Non-localized message of the backend which sould be displayed
     * if no localized message is available
     */
    val fallbackMessage: String? = null,
)
