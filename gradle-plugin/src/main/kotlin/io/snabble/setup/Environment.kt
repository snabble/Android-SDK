package io.snabble.setup

/**
 * Enum describing the environment the app is using
 */
enum class Environment(domain: String) {
    Testing("snabble-testing.io"),
    Staging("snabble-staging.io"),
    Production("snabble.io");

    /**
     * Get the base url used for every request made from the sdk
     */
    val baseUrl: String = "https://api.$domain"

    /**
     * Get a wildcard url, for certificate pinning
     */
    val wildcardUrl: String = "*.$domain"
}