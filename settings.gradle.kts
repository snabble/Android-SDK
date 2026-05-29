pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
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
