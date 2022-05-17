package io.snabble.sdk

/**
 * A notification that a violation accrued.
 */
data class ViolationNotification(
    val name: String?,
    val refersTo: String?,
    val type: String? = null,
    val fallbackMessage: String? = null,
)