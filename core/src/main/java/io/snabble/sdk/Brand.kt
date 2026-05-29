package io.snabble.sdk

import com.google.gson.annotations.SerializedName

data class Brand(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
) : Comparable<Brand> {
    override fun compareTo(other: Brand) = compareValuesBy(this, other,
        { it.id },
    )
}
