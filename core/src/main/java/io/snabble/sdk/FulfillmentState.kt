package io.snabble.sdk

import com.google.gson.annotations.SerializedName
import io.snabble.sdk.FulfillmentState

/**
 * Enum class describing the state of a fulfillment
 */
enum class FulfillmentState {
    @SerializedName("open")
    OPEN,
    @SerializedName("allocating")
    ALLOCATING,
    @SerializedName("allocated")
    ALLOCATED,
    @SerializedName("processing")
    PROCESSING,
    @SerializedName("processed")
    PROCESSED,
    @SerializedName("aborted")
    ABORTED,
    @SerializedName("allocationFailed")
    ALLOCATION_FAILED,
    @SerializedName("allocationTimedOut")
    ALLOCATION_TIMED_OUT,
    @SerializedName("failed")
    FAILED;

    /**
     * Returns true of the current fulfillment is still open and being processed
     */
    val isOpen: Boolean
        get() = when(this) {
            OPEN,
            ALLOCATING,
            ALLOCATED -> true
            else -> false
        }

    /**
     * Returns true if the current fulfillment has failed. This state is final
     */
    val isFailure: Boolean
        get() = when(this) {
            ABORTED,
            ALLOCATION_FAILED,
            ALLOCATION_TIMED_OUT,
            FAILED -> true
            else -> false
        }

    /**
     * Returns true if the current fulfillment is closed, successful or not. This state is final
     */
    val isClosed: Boolean
        get() = when(this) {
            PROCESSED,
            ABORTED,
            ALLOCATION_FAILED,
            ALLOCATION_TIMED_OUT -> true
            FAILED -> true
            else -> false
        }
}