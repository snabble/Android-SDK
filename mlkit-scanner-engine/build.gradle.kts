@file:Suppress("UnstableApiUsage")

@Suppress("DSL_SCOPE_VIOLATION") plugins {
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
        @Suppress("Deprecation")
        targetSdk = libs.versions.targetSdk.get().toInt()
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        freeCompilerArgs = listOf(
            *kotlinOptions.freeCompilerArgs.toTypedArray(),
            "-Xjvm-default=all"
        )
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugarJdkLibs)

    implementation(project(":core"))
    implementation(project(":ui"))
    implementation(project(":utils"))

    implementation(libs.google.mlkit.barcodeScanning)

    testImplementation(libs.junit)
    androidTestImplementation(libs.test.espressoCore)
    androidTestImplementation(libs.test.runner)
}
