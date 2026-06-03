pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    ":core",
    "kotlin-sample",
    ":ui",
    ":ui-toolkit",
    ":accessibility-toolbox",
    ":utils",
    ":mlkit-scanner-engine"
)
