package io.snabble.sdk

import com.google.gson.annotations.SerializedName

/**
 * Class describing a company
 */
data class Company(
    /** Get the name of the city */
    @JvmField @SerializedName("city") val city: String?,
    /** Get the name of the country */
    @JvmField @SerializedName("country") val country: String?,
    /** Get the companies name */
    @JvmField @SerializedName("name") val name: String?,
    /** Get the name of the street, including the house number */
    @JvmField @SerializedName("street") val street: String?,
    /** Get the zip code */
    @JvmField @SerializedName("zip") val zip: String?
)
