import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI

@Suppress("DSL_SCOPE_VIOLATION") plugins {
    alias(libs.plugins.benManesVersions)
    alias(libs.plugins.versionCatalogUpdate)
    alias(libs.plugins.dokka)
    id("maven-publish")
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath(libs.classpath.androidGradlePlugin)
        classpath(libs.classpath.bjoernq.unmockPlugin)
        classpath(libs.classpath.dokkaBase)
        classpath(libs.classpath.dokkaGradlePlugin)
        classpath(libs.classpath.jlouns.gradleCrossPlatformExecPlugin)
        classpath(libs.classpath.kotlinAndroidPlugin)
        classpath(libs.classpath.qmazzo.sqlitePlugin)
    }
}

val sdkVersion = libs.versions.snabbleSdk.get()

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://datatrans.jfrog.io/artifactory/mobile-sdk/")
        maven(url = "https://jitpack.io")
    }

    project.extra.apply {
        set(
            "sdkVersion",
            (System.getenv("SDK_VERSION_NAME")?.replace("v", "") ?: "dev") +
                    (project.properties["versionSuffix"] ?: "")
        )
    }
}

afterEvaluate {
    publishing {
        repositories {
            maven {
                name = "LocalBuildDir"
                val releasesRepoUrl = URI.create("file://${project.rootDir.absolutePath}/build/maven-releases/")
                val snapshotsRepoUrl = URI.create("file://${project.rootDir.absolutePath}/build/maven-snapshots/")
                url = if (project.extra.get("sdkVersion").toString().endsWith("SNAPSHOT")) {
                    snapshotsRepoUrl
                } else {
                    releasesRepoUrl
                }
            }

            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/snabble/Android-SDK")

                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}

tasks.named("dokkaHtml", DokkaTask::class.java).configure {
    dokkaSourceSets {
        configureEach {
            skipDeprecated.set(true)
        }
    }
}

tasks.withType(AbstractDokkaTask::class.java) {
    outputDirectory.set(rootDir.toPath().resolve("docs").toFile())
    suppressObviousFunctions.set(true)
    moduleName.set("snabble Android SDK API Documentation")
    val escapedLogoPath = file("dokka/assets/logo-icon.svg").absolutePath.replace("\\\\", "\\\\\\\\")
    pluginsMapConfiguration.set(
        mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """
        {
            "footerMessage": "© 2022 snabble GmbH",
            "customAssets": [
                "$escapedLogoPath"
            ]
        }
    """
        )
    )
}

tasks.register("printVersion") {
    println(libs.versions.snabbleSdk)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

versionCatalogUpdate {
    sortByKey.set(false)
    keep {
        keepUnusedVersions.set(true)
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
