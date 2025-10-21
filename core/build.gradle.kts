@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(libs.plugins.androidLibrary.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
}

apply {
    from("../scripts/maven.gradle")
}

description = "Snabble Core: The business logic of the Snabble SDK"

android {
    namespace = "io.snabble.sdk"

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFile("proguard-rules.pro")
        buildConfigField("String", "VERSION_NAME", "\"${project.extra.get("sdkVersion")}\"")

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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugarJdkLibsNio)

    implementation(project(":utils"))

    //noinspection GradleDependency
    implementation(libs.apache.commonsLang3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.caverock.androidsvgAar)
    implementation(libs.commonsIo)
    implementation(libs.iban4j)
    implementation(libs.googlePlayServices.wallet)
    api(libs.gson)
    implementation(libs.rekisoftLazyWorker)

    api(libs.squareup.okhttp3.okhttp)
    implementation(libs.squareup.okhttp3.loggingInterceptor)

    testImplementation(libs.junit)
    testImplementation(libs.roboletric)
    testImplementation(libs.squareup.okhttp3.mockwebserver)
    testImplementation(libs.koltin.reflect)
    testImplementation(libs.androidx.coreTesting)

    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
