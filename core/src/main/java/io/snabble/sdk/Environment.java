package io.snabble.sdk;

public enum Environment {
    TESTING("snabble-testing.io"),
    STAGING("snabble-staging.io"),
    PRODUCTION("snabble.io");

    private final String baseUrl;
    private final String wildcardUrl;

    Environment(String domain) {
        this.baseUrl = "https://api." + domain;
        this.wildcardUrl = "*." + domain;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getWildcardUrl() {
        return wildcardUrl;
    }

    public static Environment getEnvironmentByUrl(String url) {
        for (Environment environment : Environment.values()) {
            if (url.startsWith(environment.getBaseUrl())) {
                return environment;
            }
        }

        return TESTING;
    }
}