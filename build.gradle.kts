import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI

plugins {
    id("maven-publish")
    id("org.jetbrains.dokka")
}

buildscript {

    repositories {
        google()
        mavenCentral()
        maven(url = "https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.7.20")
        classpath("org.jetbrains.dokka:dokka-base:1.7.20")
        classpath("com.android.tools.build:gradle:7.4.1")
        classpath("gradle.plugin.com.github.jlouns:gradle-cross-platform-exec-plugin:0.5.0")
        classpath("gradle.plugin.gmazzo:sqlite-plugin:0.2")
        classpath("com.github.bjoernq:unmockplugin:0.7.9")
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
        val suffix = project.properties.getOrElse("versionSuffix") { "" }
        set("sdkVersion", "${sdkVersion}$suffix")
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
