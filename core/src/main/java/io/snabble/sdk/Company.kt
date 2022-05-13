package io.snabble.sdk

/**
 * Class describing a company
 */
data class Company(
    /**
     * Get the name of the city
     */
    @JvmField
    val city: String?,
    /**
     * Get the name of the country
     */
    @JvmField
    val country: String?,
    /**
     * Get the companies name
     */
    @JvmField
    val name: String?,
    /**
     * Get the name of the street, including the house number
     */
    @JvmField
    val street: String?,
    /**
     * Get the zip code
     */
    @JvmField
    val zip: String?
)