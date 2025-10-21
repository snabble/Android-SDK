@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(libs.plugins.androidLibrary.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

apply {
    from("../scripts/maven.gradle")
}

description = "Snabble UI-Toolkit: Additional views for simple and sample apps using the Snabble SDK"

android {
    namespace = "io.snabble.sdk.ui.toolkit"

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        targetSdk = libs.versions.targetSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
            freeCompilerArgs.addAll(
                "-Xjvm-default=all"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        disable.add("MissingTranslation")
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

}

dependencies {
    implementation(project(":accessibility-toolbox"))
    implementation(project(":utils"))
    implementation(project(":ui"))

    implementation(libs.android.material)
    implementation(libs.androidx.activityCompose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayoutCompose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.navigation.fragmentKtx)
    implementation(libs.androidx.navigation.runtimeKtx)
    api(libs.androidx.preferences)
    implementation(libs.androidx.startupRuntime)
    implementation(libs.commonsIo)
    implementation(libs.googlePlayServices.maps)
    implementation(libs.kotlinx.serializationJson)
    implementation(libs.picasso)
    implementation(libs.sebaslogen.resaca)

    implementation(libs.bundles.compose)
    implementation(libs.androidx.swiperefreshlayout)
    debugImplementation(libs.bundles.compose.debug)
    implementation("androidx.compose.material:material-icons-core:1.7.8")

    implementation(libs.bundles.koin)

    testImplementation(libs.kotest.assertionsCore)
    testImplementation(libs.kotest.runnerJunit)
    testImplementation(libs.mockk)
}
