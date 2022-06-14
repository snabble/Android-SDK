package io.snabble.setup

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The snabble gradle plugin.
 */
open class SnabblePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("snabble", SnabbleExtension::class.java, project)

        // inject maven repositories
        project.repositories.maven { repo ->
            repo.name = "Snabble Maven Repository"
            repo.setUrl("https://raw.githubusercontent.com/snabble/maven-repository/releases")
            repo.content {
                it.includeGroup("io.snabble.sdk")
                it.includeModule("com.paypal.android", "risk-component")
                it.includeModule("com.samsung.android.sdk", "samsungpay")
                it.includeModule("ch.datatrans", "android-sdk")
                it.includeModule("ch.twint.payment", "twint-sdk-android")
            }
        }
        project.repositories.maven { repo ->
            repo.name = "Datatrans Maven Repository"
            repo.setUrl("https://datatrans.jfrog.io/artifactory/mobile-sdk/")
        }
    }
}