package io.snabble.setup

/**
 * Enum describing the environment the app is using
 */
enum class Environment(domain: String) {
    TESTING("snabble-testing.io"),
    STAGING("snabble-staging.io"),
    PRODUCTION("snabble.io");

    /**
     * Get the base url used for every request made from the sdk
     */
    val baseUrl: String = "https://api.$domain"

    /**
     * Get a wildcard url, for certificate pinning
     */
    val wildcardUrl: String = "*.$domain"

    companion object {
        /**
         * Determine the environment by a url string
         */
        fun getEnvironmentByUrl(url: String): Environment {
            for (environment in values()) {
                if (url.startsWith(environment.baseUrl)) {
                    return environment
                }
            }
            return TESTING
        }
    }
}