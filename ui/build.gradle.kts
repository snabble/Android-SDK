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

description = "Snabble UI: The Android User Interface Snabble SDK"

android {
    namespace = "io.snabble.sdk.ui"

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

    lint {
        disable.addAll(listOf("LabelFor", "MissingTranslation"))
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugarJdkLibsNio)

    implementation(project(":utils"))
    api(project(":core"))
    api(project(":accessibility-toolbox"))

    implementation(libs.airbnb.lottie)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.lifecycleLiveData)
    implementation(libs.android.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.startupRuntime)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.webkit)
    implementation(libs.commonsIo)
    implementation(libs.compose.iconsExtended)
    implementation(libs.datatrans.androidSdk)
    implementation(libs.glide.compose)
    implementation(libs.google.zxing.core)
    implementation(libs.googlePlayServices.wallet)
    implementation(libs.picasso)
    implementation(libs.kotlinx.serializationJson)
    implementation(libs.koltin.reflect)
    implementation(libs.rekisoftLazyWorker)
    implementation(libs.relex.circleindicator)
    implementation(libs.snabble.phoneAuth.countryCodePicker)
    implementation(libs.bundles.camera)
    implementation(libs.bundles.navigation)
    implementation("androidx.compose.material:material-icons-core:1.7.8")

    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    testImplementation(libs.kotest.assertionsCore)
    testImplementation(libs.kotest.runnerJunit)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.test.espressoCore)
    androidTestImplementation(libs.test.runner)
}
