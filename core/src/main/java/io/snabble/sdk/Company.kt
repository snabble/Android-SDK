package io.snabble.sdk

/**
 * Class describing a company
 */
data class Company(
    /**
     * Get the name of the city
     */
    val city: String,
    /**
     * Get the name of the country
     */
    val country: String,
    /**
     * Get the companies name
     */
    val name: String,
    /**
     * Get the name of the street, including the house number
     */
    val street: String,
    /**
     * Get the zip code
     */
    val zip: String
)