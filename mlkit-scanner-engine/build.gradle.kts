@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.dokka")
}

apply {
    from("../scripts/maven.gradle")
}

description = "ML Kit Scanner Engine: The ML Kit scanner engine implementation for the snabble SDK"

android {
    namespace = "io.snabble.sdk.firebase"


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
            "-Xjvm-default=all"
        )
        jvmTarget = "1.8"
    }

    lint{
        abortOnError = false
    }
}

dependencies {
    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:1.1.5")

    implementation (project(":core"))
    implementation (project(":ui"))
    implementation (project(":utils"))
    implementation ("com.google.mlkit:barcode-scanning:17.0.2")

    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test:runner:1.4.0")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")
}
