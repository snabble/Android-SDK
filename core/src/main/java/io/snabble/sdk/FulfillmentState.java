package io.snabble.sdk;

import com.google.gson.annotations.SerializedName;

public enum FulfillmentState {
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

    public boolean isOpen() {
        return this == OPEN
                || this == ALLOCATING
                || this == ALLOCATED;
    }

    public boolean isFailure() {
        return this == ABORTED
                || this == ALLOCATION_FAILED
                || this == ALLOCATION_TIMED_OUT
                || this == FAILED;
    }

    public boolean isClosed() {
        return this == PROCESSED
                || this == ABORTED
                || this == ALLOCATION_FAILED
                || this == ALLOCATION_TIMED_OUT
                || this == FAILED;
    }
}
