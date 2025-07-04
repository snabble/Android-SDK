@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(libs.plugins.androidLibrary.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
}

apply {
    from("../scripts/maven.gradle")
}

description = "ML Kit Scanner Engine: The ML Kit scanner engine implementation for the Snabble SDK"

android {
    namespace = "io.snabble.sdk.firebase"

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
            postprocessing {
                isRemoveUnusedCode = false
                isRemoveUnusedResources = false
                isObfuscate = false
                isOptimizeCode = false
                proguardFile("proguard-rules.pro")
            }
        }
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


    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ui"))
    implementation(project(":utils"))

    implementation(libs.google.mlkit.barcodeScanning)

    testImplementation(libs.junit)
    androidTestImplementation(libs.test.espressoCore)
    androidTestImplementation(libs.test.runner)
}
