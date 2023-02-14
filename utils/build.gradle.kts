@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.dokka")
    id("de.mobilej.unmock")
}

apply {
    from("../scripts/maven.gradle")
}

description = "Snabble Utils: Util collection for the snabble SDK integration"

android {
    namespace = "io.snabble.sdk.utils"

    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 32
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        freeCompilerArgs = listOf(
            *kotlinOptions.freeCompilerArgs.toTypedArray(),
            "-Xjvm-default=all",
            "-opt-in=kotlin.RequiresOptIn"
        )
        jvmTarget = "1.8"
    }
}

dependencies {
    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:1.1.5")

    //noinspection GradleDependency
    implementation ("commons-io:commons-io:2.5")
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("com.squareup.okhttp3:okhttp-tls:4.10.0")
    implementation ("androidx.appcompat:appcompat:1.3.1")
    implementation ("com.google.code.gson:gson:2.9.1")
    implementation ("androidx.core:core-ktx:1.8.0")

    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")

    // for testing
    testImplementation ("junit:junit:4.13.2")
    testImplementation ("org.mockito.kotlin:mockito-kotlin:4.0.0")
    unmock ("org.robolectric:android-all:4.3_r2-robolectric-0")
}

unMock {
    keep ("android.text.style.URLSpan")
}
