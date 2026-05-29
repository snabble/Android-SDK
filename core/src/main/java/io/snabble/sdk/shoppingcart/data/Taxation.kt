package io.snabble.sdk.shoppingcart.data

import com.google.gson.annotations.SerializedName

/**
 * Enum describing the type of taxation
 */
enum class Taxation(val value: String) {

    @SerializedName("undecided") UNDECIDED("undecided"),
    @SerializedName("inHouse") IN_HOUSE("inHouse"),
    @SerializedName("takeaway") TAKEAWAY("takeaway")
}

