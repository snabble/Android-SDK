@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(libs.plugins.androidLibrary.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id(libs.plugins.unmock.get().pluginId)
}

apply {
    from("../scripts/maven.gradle")
}

description = "Snabble Utils: Util collection for the Snabble SDK integration"

android {
    namespace = "io.snabble.sdk.utils"

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
        isCoreLibraryDesugaringEnabled = true
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
}

dependencies {
    coreLibraryDesugaring(libs.desugarJdkLibsNio)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.commonsIo)
    implementation(libs.gson)
    implementation(libs.squareup.okhttp3.okhttp)
    implementation(libs.squareup.okhttp3.tls)
    implementation(libs.google.zxing.core)

    // for testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.espressoCore)
    unmock(libs.roboletric.androidAll)
}

unMock {
    keep("android.text.style.URLSpan")
}
