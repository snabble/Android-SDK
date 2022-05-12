package io.snabble.sdk

enum class Environment(domain: String) {
    TESTING("snabble-testing.io"),
    STAGING("snabble-staging.io"),
    PRODUCTION("snabble.io");

    val baseUrl: String = "https://api.$domain"
    val wildcardUrl: String = "*.$domain"

    companion object {
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