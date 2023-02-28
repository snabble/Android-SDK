pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.dokka") version "1.6.21"
        id("com.android.application") version "7.4.1"
        id("org.jetbrains.kotlin.android") version "1.6.21"
    }
}

include(
    ":core",
    ":java-sample",
    "kotlin-sample",
    "kotlin-customization-sample",
    ":ui",
    ":ui-toolkit",
    ":accessibility-toolbox",
    ":utils",
    ":mlkit-scanner-engine",
    ":kotlin-compose-sample"
)